package com.pinwheel.core.model

/** Immutable edit recipes and existing source references, never nested histories or media copies. */
data class VideoEdit(val name:String,val clips:List<Clip>,val video:VideoProjectEdits,val adjustments:Adjustments=Adjustments()) {
    companion object { fun of(project:StudioProject)=VideoEdit(project.name,project.clips,project.video,project.adjustments) }
    fun applyTo(project:StudioProject)=project.copy(name=name,clips=clips,video=video,adjustments=adjustments)
}

data class VideoHistory(val past:List<VideoEdit> = emptyList(),val future:List<VideoEdit> = emptyList()) {
    fun record(project:StudioProject)=VideoHistory((past+VideoEdit.of(project)).takeLast(LIMIT))
    fun undo(project:StudioProject):StudioProject?=past.lastOrNull()?.let {
        it.applyTo(project).copy(videoHistory=VideoHistory(past.dropLast(1),(future+VideoEdit.of(project)).takeLast(LIMIT)))
    }
    fun redo(project:StudioProject):StudioProject?=future.lastOrNull()?.let {
        it.applyTo(project).copy(videoHistory=VideoHistory((past+VideoEdit.of(project)).takeLast(LIMIT),future.dropLast(1)))
    }
    companion object { const val LIMIT=40 }
}
