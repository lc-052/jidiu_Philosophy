package com.xiaojidiu.philosophy.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import com.xiaojidiu.philosophy.domain.model.*
import com.xiaojidiu.philosophy.ui.components.*

@Composable fun PhilosopherCard(philosopher: Philosopher, selected: Boolean?=null, onClick: ()->Unit) {
    if(selected!=null) {
        XjdCard {
            PhilosopherAvatar(philosopher.id,name=philosopher.name)
            Text(philosopher.name,style=MaterialTheme.typography.titleMedium)
            Text(philosopher.intro,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
            OutlinedButton(onClick,modifier=Modifier.fillMaxWidth().testTag("invite_${philosopher.id}")) {
                Text(if(selected) "✓ 已入座" else "邀请入座")
            }
        }
    } else {
        Surface(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag("invite_${philosopher.id}"),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow) {
            Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                PhilosopherAvatar(philosopher.id,name=philosopher.name)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(philosopher.name,style=MaterialTheme.typography.titleMedium)
                    Text(philosopher.intro,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"查看哲学家",tint=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable fun SchoolCard(school: School, open: ()->Unit) {
    Surface(onClick=open,shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(school.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"查看流派",tint=MaterialTheme.colorScheme.primary)
            }
            Text(school.intro,style=MaterialTheme.typography.bodyMedium,maxLines=3,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun ExploreScreen(library: Library, question: (String)->Unit, philosopher: (String)->Unit, school: (String)->Unit, map: ()->Unit, games: ()->Unit={}, tips: ()->Unit={}) {
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf("全部") }
    val needle=query.trim()
    val labels=listOf("脑洞问题","哲学家","思想流派","人生问题")
    val questions=library.questions.filter { q ->
        (section!=3 || q.category in listOf("人生意义","幸福","死亡","身份","社会")) &&
        (category=="全部" || q.category==category) &&
        (needle.isEmpty() || listOf(q.title,q.scenario,q.category,q.background,q.source).any {it.contains(needle,true)})
    }
    val philosophers=library.philosophers.filter { p -> needle.isEmpty() || listOf(p.name,p.intro,p.ideas,p.focus,p.region).any {it.contains(needle,true)} }
    val schools=library.schools.filter { s -> needle.isEmpty() || listOf(s.name,s.intro,s.ideas,s.misconception).any {it.contains(needle,true)} }
    val count=when(section) {1->philosophers.size;2->schools.size;else->questions.size}
    LazyColumn(Modifier.testTag("explore_list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { PageHeading("随便逛逛","从一个问题，也能走进一种思想。") }
        item {
            OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().testTag("explore_search"),placeholder={Text("搜索当前分类……")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Outlined.Close,"清空搜索")}},singleLine=true,shape=MaterialTheme.shapes.medium)
        }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                itemsIndexed(labels) {i,label->FilterChip(section==i,{section=i;category="全部"},label={Text(label)})}
            }
        }
        if(section==0 || section==3) item {
            val categories=library.questions.filter {section!=3 || it.category in listOf("人生意义","幸福","死亡","身份","社会")}.map {it.category}.distinct()
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                items(listOf("全部")+categories) {name->FilterChip(category==name,{category=name},label={Text(name)})}
            }
        }
        if(section==1 || section==2) item {
            NavigationRow("这些人到底什么关系？","沿着时代与分歧，看看思想的相遇",Icons.Outlined.AccountTree,map)
        }
        item { NavigationRow("TIPS · 哲学词语","形而上学、自由意志……留一点自己的理解",Icons.Outlined.AutoStories,tips) }
        item { NavigationRow("游戏中的哲学","按作品读：素晴日、樱之诗",Icons.Outlined.SportsEsports,games) }
        item { Text("${if(needle.isEmpty()) "${labels[section]} ·" else "搜索结果 ·"} $count ${if(section==1) "位" else if(section==2) "种" else "个"}",Modifier.padding(top=8.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        when(section) {
            0,3 -> {
                if(questions.isEmpty()) item { EmptyState("没找到这个念头，换个词试试。") }
                items(questions,key={it.id}) {q->
                    Surface(onClick={question(q.id)},shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("${q.category} · ${if(q.difficulty==1) "轻轻想" else "多想两下"}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text(q.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,"打开问题",tint=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(q.scenario,style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            1 -> {
                if(philosophers.isEmpty()) item { EmptyState("没找到这位，换个词试试。") }
                items(philosophers,key={it.id}) { PhilosopherCard(it) {philosopher(it.id)} }
            }
            else -> {
                if(schools.isEmpty()) item { EmptyState("没找到这个思想，换个词试试。") }
                items(schools,key={it.id}) { SchoolCard(it) {school(it.id)} }
            }
        }
    }
}


