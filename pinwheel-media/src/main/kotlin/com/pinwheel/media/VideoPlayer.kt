package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.StudioProject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

data class PresentedFrame(val timeUs:Long,val generation:Long,val pixels:RgbaFrame)
data class PlaybackStatus(val timeUs:Long,val generation:Long,val playing:Boolean,val ready:Boolean,val ended:Boolean,
    val renderedFrames:Long,val droppedFrames:Long,val error:Throwable?)

/** Audio and GL have independent owners. Slow video is dropped; it cannot hold the PCM clock. */
class VideoPlayer(project:StudioProject,private val evaluatorFactory:(StudioProject)->VideoFrameEvaluator,
    private val decoderFactory:MediaDecoderFactory,private val outputFactory:()->PcmOutput={JavaSoundOutput()},
    private val frameRate:Int=30,private val onFrame:(PresentedFrame)->Unit={}) : AutoCloseable {
    private val lock=ReentrantLock();private val changed=lock.newCondition()
    private var project=project
    private var generation=0L
    private var seekGeneration=0L
    private var audioSeekGeneration=-1L
    private var requestedUs=0L
    private var baseUs=0L
    private var baselineFrames=0L
    private var output:PcmOutput?=null
    private var playing=false
    private var ready=false
    private var ended=false
    private var closed=false
    private var error:Throwable?=null
    private var rendered=0L
    private var dropped=0L
    private var pendingProject:StudioProject?=null
    private var updateDue=0L
    @Volatile var latestFrame:PresentedFrame?=null
        private set
    private val audioWorker=thread(start=false,isDaemon=true,name="pinwheel-player-audio") { guard { audioLoop() } }
    private val renderWorker=thread(start=false,isDaemon=true,name="pinwheel-player-render") { guard { renderLoop() } }
    init { require(frameRate in 1..60 && project.clips.isNotEmpty());audioWorker.start();renderWorker.start() }
    private fun timeLocked():Long = if(ended)project.durationMs*1000 else
        if(audioSeekGeneration!=seekGeneration)requestedUs else (baseUs+((output?.consumedFrames ?: baselineFrames)-baselineFrames).coerceAtLeast(0)*1_000_000/48000).coerceIn(0,project.durationMs*1000)
    val status get()=lock.withLock { PlaybackStatus(timeLocked(),generation,playing,ready,ended,rendered,dropped,error) }
    fun play()=lock.withLock { checkOpen();if(ended)seekLocked(0);playing=true;changed.signalAll() }
    fun pause()=lock.withLock { checkOpen();playing=false;changed.signalAll() }
    fun seek(timeUs:Long):Long=lock.withLock { checkOpen();seekLocked(timeUs) }
    private fun seekLocked(timeUs:Long):Long {
        requestedUs=timeUs.coerceIn(0,(project.durationMs*1000-1).coerceAtLeast(0));generation++;seekGeneration++
        ready=false;ended=false;latestFrame=null;changed.signalAll();return generation
    }
    /** Coalesces edit bursts for 120 ms; plain live-overlay changes need no GPU/audio rebuild. */
    fun updateProject(snapshot:StudioProject)=lock.withLock { checkOpen();pendingProject=snapshot;updateDue=System.nanoTime()+120_000_000;changed.signalAll() }
    private fun applyPending() {
        val pending=pendingProject ?: return
        if(System.nanoTime()<updateDue)return
        val at=timeLocked();val recipeChanged=audioSignature(project)!=audioSignature(pending) || renderSignature(project)!=renderSignature(pending)
        project=pending;pendingProject=null
        if(!recipeChanged) { changed.signalAll();return }
        requestedUs=at.coerceAtMost((project.durationMs*1000-1).coerceAtLeast(0))
        generation++;seekGeneration++;ready=false;ended=false;changed.signalAll()
    }
    private fun checkOpen() { check(!closed) { "Player is closed" };error?.let { throw IllegalStateException("Playback failed",it) } }
    private fun guard(block:()->Unit) { try { block() } catch(failure:Throwable) { lock.withLock { if(!closed) { error=failure;playing=false };changed.signalAll() } } }
    private fun audioLoop() {
        var mixer:TimelineAudioMixer?=null;var snapshot:StudioProject?=null;var seek=-1L;var nextSample=0L
        outputFactory().use { sink ->
            lock.withLock { output=sink;baselineFrames=sink.consumedFrames;changed.signalAll() }
            try {
                while(true) {
                    val control=lock.withLock {
                        if(closed || error!=null)return
                        applyPending()
                        if(seek==seekGeneration)null else { sink.stop();sink.flush();Triple(project,seekGeneration,requestedUs) }
                    }
                    if(control!=null) {
                        if(snapshot?.let { audioSignature(it)==audioSignature(control.first) } != true) { mixer?.close();mixer=TimelineAudioMixer(control.first,decoderFactory) }
                        mixer?.seek();snapshot=control.first;seek=control.second
                        lock.withLock { if(seek==seekGeneration) { baseUs=control.third;baselineFrames=sink.consumedFrames;nextSample=baseUs*48000/1_000_000;audioSeekGeneration=seek;changed.signalAll() } }
                    }
                    val task=lock.withLock {
                        if(closed || error!=null)return
                        applyPending()
                        if(audioSeekGeneration!=seekGeneration) null
                        else if(!playing) { sink.stop();changed.awaitNanos(5_000_000);null }
                        else { sink.start();val at=timeLocked()
                            if(at>=project.durationMs*1000) { ended=true;playing=false;sink.stop();changed.signalAll();null }
                            else if(sink.availableFrames<480) { changed.awaitNanos(2_000_000);null }
                            else Triple(seekGeneration,nextSample,minOf(960,sink.availableFrames,(project.durationMs*48-nextSample).coerceIn(0,960).toInt()))
                        }
                    } ?: continue
                    if(task.third==0) { lock.withLock { changed.awaitNanos(2_000_000) };continue }
                    val pcm=mixer!!.renderSamples(task.second,task.third)
                    lock.withLock {
                        if(task.first==seekGeneration && !closed && playing) { sink.write(pcm,task.third);nextSample+=task.third;changed.signalAll() }
                    }
                }
            } finally { mixer?.close();lock.withLock { output=null } }
        }
    }
    private fun renderLoop() {
        var evaluator:VideoFrameEvaluator?=null;var snapshot:StudioProject?=null;var shown=-1L;var previousTick=-1L
        try {
            while(true) {
                val task=lock.withLock {
                    if(closed || error!=null)return
                    applyPending();val at=timeLocked();val tick=at*frameRate/1_000_000
                    if(output==null || ended || (shown==generation && tick==previousTick) || (!playing && shown==generation)) { changed.awaitNanos(5_000_000);null }
                    else Triple(project,generation,at.coerceAtMost((project.durationMs*1000-1).coerceAtLeast(0)))
                } ?: continue
                if(snapshot?.let { renderSignature(it)==renderSignature(task.first) } != true) { evaluator?.close();evaluator=evaluatorFactory(task.first);snapshot=task.first }
                if(shown!=task.second) { evaluator!!.resetHistory();previousTick=-1 }
                val pixels=evaluator!!.render(task.third)
                val frame=PresentedFrame(task.third,task.second,pixels)
                val publish=lock.withLock {
                    if(task.second!=generation || closed)false else {
                        val tick=task.third*frameRate/1_000_000
                        if(previousTick>=0 && tick>previousTick+1)dropped+=tick-previousTick-1
                        previousTick=tick;shown=generation;latestFrame=frame;ready=true;rendered++;changed.signalAll();true
                    }
                }
                if(publish)onFrame(frame)
            }
        } finally { evaluator?.close() }
    }
    override fun close() {
        lock.withLock { if(closed)return;closed=true;playing=false;changed.signalAll() }
        audioWorker.join(10_000);renderWorker.join(10_000)
        check(!audioWorker.isAlive && !renderWorker.isAlive) { "Playback workers did not stop" }
    }
    companion object {
        private fun audioSignature(p:StudioProject)=p.clips.map { listOf(it.id,it.uri,it.startMs,it.endMs,it.muted,it.playbackSpeed,it.video.volume) } to p.video.audio
        private fun renderSignature(p:StudioProject)=p.clips to p.video.copy(texts=emptyList(),captions=emptyList(),images=p.video.images.filter { it.video!=null || p.video.effects.any { effect->effect.target==it.id } || it.uri.startsWith("overlay:") })
    }
}
