package matt.obs.tempcommon.tempexpect

import matt.lang.anno.JetBrainsYouTrackProject.KT
import matt.lang.anno.YouTrackIssue
import matt.obs.bindings.bool.ObsB
import matt.obs.col.olist.ImmutableObsList
import matt.obs.col.olist.dynamic.SortedFilteredList

@YouTrackIssue(KT, 65555)
actual fun <E> dynamicList(
    source: ImmutableObsList<E>,
    filter: ((E) -> Boolean)?,
    dynamicFilter: ((E) -> ObsB)?,
    comparator: Comparator<in E>?
): SortedFilteredList<E> = error("not until beta5...")
