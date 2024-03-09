package matt.obs.tempcommon.tempexpect

import matt.lang.anno.JetBrainsYouTrackProject.KT
import matt.lang.anno.YouTrackIssue
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.dynamic.BasicFilteredList


@YouTrackIssue(KT, 65555)
expect fun <E> dynamicList(
    source: ImmutableObsList<E>,
    filter: ((E) -> Boolean)? = null,
    dynamicFilter: ((E) -> ObsB)? = null,
    comparator: Comparator<in E>? = null
): BasicFilteredList<E>
