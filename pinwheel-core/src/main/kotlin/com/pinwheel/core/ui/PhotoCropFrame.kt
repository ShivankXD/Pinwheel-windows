package com.pinwheel.core.ui
import com.pinwheel.core.model.Adjustments

/** Clockwise corners 0..3, top/right/bottom/left edges 4..7; -1 moves the frame. */
fun resizeCropFrame(a:Adjustments,handle:Int,dx:Float,dy:Float):Adjustments {
 var l=a.cropLeft;var t=a.cropTop;var r=a.cropRight;var b=a.cropBottom
 if(handle == -1){val x=dx.coerceIn(-l,1-r);val y=dy.coerceIn(-t,1-b);l+=x;r+=x;t+=y;b+=y}
 else if(handle in 0..7){
  if(handle==0||handle==3||handle==7)l=(l+dx).coerceIn(0f,(r-.08f).coerceAtLeast(0f))
  if(handle==1||handle==2||handle==5)r=(r+dx).coerceIn((l+.08f).coerceAtMost(1f),1f)
  if(handle==0||handle==1||handle==4)t=(t+dy).coerceIn(0f,(b-.08f).coerceAtLeast(0f))
  if(handle==2||handle==3||handle==6)b=(b+dy).coerceIn((t+.08f).coerceAtMost(1f),1f)
 }else return a
 return a.copy(crop=if(handle>=0)"Free"else a.crop,cropLeft=l,cropTop=t,cropRight=r,cropBottom=b)
}
