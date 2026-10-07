package com.xiaojidiu.philosophy.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val userAvatarChoices=listOf("sprout" to "小芽", "moon" to "月亮", "cat" to "小猫", "cloud" to "云朵", "sun" to "太阳", "planet" to "星球")

/** Original geometric portraits, not historical likenesses or downloaded photographs. */
@Composable fun PhilosopherAvatar(id: String, modifier: Modifier=Modifier, size: Dp=56.dp, name: String?=null) {
    val ids=listOf("socrates","plato","aristotle","epicurus","marcus","descartes","hume","kant","schopenhauer","nietzsche","kierkegaard","sartre","camus","wittgenstein","rawls","nozick","laozi","zhuangzi","confucius","mencius","mill","epictetus")
    val eastern=id in setOf("laozi","zhuangzi","confucius","mencius")
    val index=ids.indexOf(id).let {if(it<0) Math.floorMod(id.hashCode(),22) else it}
    val palette=listOf(Color(0xFFE4DFFF),Color(0xFFD5EFFD),Color(0xFFCDF3DD),Color(0xFFFFEAA4),Color(0xFFFFDCD4))
    Canvas(modifier.size(size).clip(CircleShape).semantics {contentDescription="${name ?: "思想家"}的抽象头像"}) {
        val w=this.size.width;val ink=Color(0xFF373044); val skin=Color(0xFFFFE2C7)
        fun point(x: Float,y: Float)=Offset(w*x,w*y)
        drawCircle(palette[index%palette.size],w*.5f,point(.5f,.5f))
        drawOval(ink,point(.16f,.76f),Size(w*.68f,w*.4f))
        drawOval(skin,point(.25f,.19f),Size(w*.5f,w*.61f))
        val hair=if(index%3==0) Color(0xFFE9E3F3) else ink
        if(index%4!=0) drawArc(hair,180f,180f,true,point(.22f,.13f),Size(w*.56f,w*.44f))
        if(eastern) {
            drawCircle(ink,w*.085f,point(.5f,.16f))
            drawLine(ink,point(.25f,.27f),point(.75f,.27f),w*.065f)
        }
        if(index%4==0 || id=="sartre") {
            drawCircle(ink,w*.09f,point(.38f,.46f),style=Stroke(w*.02f));drawCircle(ink,w*.09f,point(.62f,.46f),style=Stroke(w*.02f))
            drawLine(ink,point(.47f,.46f),point(.53f,.46f),w*.02f)
        }
        drawCircle(ink,w*.023f,point(.38f,.46f));drawCircle(ink,w*.023f,point(.62f,.46f))
        drawLine(ink,point(.5f,.5f),point(.47f,.58f),w*.018f)
        if(index%3==0 || eastern) {
            drawPath(Path().apply {moveTo(w*.3f,w*.64f);quadraticTo(w*.5f,w*(if(eastern) .98f else .85f),w*.7f,w*.64f);close()},hair)
        }
        if(id=="nietzsche" || index%5==3) {
            drawOval(ink,point(.31f,.59f),Size(w*.38f,w*.09f))
        } else drawArc(ink,0f,180f,false,point(.42f,.59f),Size(w*.16f,w*.08f),style=Stroke(w*.02f))
        // A distinct lapel accent keeps every portrait recognizable at small sizes.
        drawCircle(palette[(index+2)%palette.size],w*.035f,point(.34f,.86f))
    }
}

@Composable fun UserAvatar(id: String, modifier: Modifier=Modifier, size: Dp=56.dp) {
    val index=userAvatarChoices.indexOfFirst {it.first==id}.coerceAtLeast(0)
    val palette=listOf(Color(0xFFCDF3DD),Color(0xFFE4DFFF),Color(0xFFFFDCD4),Color(0xFFD5EFFD),Color(0xFFFFEAA4),Color(0xFFE4DFFF))
    Canvas(modifier.size(size).clip(CircleShape).semantics {contentDescription="${userAvatarChoices[index].second}头像"}) {
        val w=this.size.width; val ink=Color(0xFF373044)
        fun p(x: Float,y: Float)=Offset(w*x,w*y)
        drawCircle(palette[index],w*.5f,p(.5f,.5f))
        when(index) {
            0->{drawLine(ink,p(.5f,.35f),p(.5f,.22f),w*.035f);drawOval(ink,p(.28f,.12f),Size(w*.22f,w*.12f));drawOval(ink,p(.5f,.08f),Size(w*.23f,w*.13f))}
            1->drawArc(ink,220f,230f,false,p(.19f,.16f),Size(w*.62f,w*.65f),style=Stroke(w*.035f))
            2->{drawPath(Path().apply {moveTo(w*.25f,w*.4f);lineTo(w*.23f,w*.2f);lineTo(w*.43f,w*.33f);moveTo(w*.57f,w*.33f);lineTo(w*.77f,w*.2f);lineTo(w*.75f,w*.4f)},ink,style=Stroke(w*.035f));drawLine(ink,p(.2f,.56f),p(.32f,.59f),w*.025f);drawLine(ink,p(.68f,.59f),p(.8f,.56f),w*.025f)}
            3->{drawCircle(Color.White,w*.18f,p(.32f,.49f));drawCircle(Color.White,w*.23f,p(.5f,.4f));drawCircle(Color.White,w*.18f,p(.68f,.49f))}
            4->{repeat(8) {i->val a=i*Math.PI/4;val x=kotlin.math.cos(a).toFloat();val y=kotlin.math.sin(a).toFloat();drawLine(ink,p(.5f+x*.32f,.5f+y*.32f),p(.5f+x*.4f,.5f+y*.4f),w*.025f)}}
            5->drawOval(ink,p(.12f,.36f),Size(w*.76f,w*.3f),style=Stroke(w*.035f))
        }
        drawCircle(ink,w*.025f,p(.4f,.48f));drawCircle(ink,w*.025f,p(.6f,.48f))
        drawArc(ink,0f,180f,false,p(.42f,.56f),Size(w*.16f,w*.09f),style=Stroke(w*.025f))
    }
}



