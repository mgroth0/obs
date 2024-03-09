package matt.obs.col.change.atomic

import matt.lang.common.NEVER
import matt.log.taball
import matt.model.data.index.toKotlinIndexedValue
import matt.model.data.index.withIndex
import matt.obs.col.change.AddAt
import matt.obs.col.change.AtomicListChange
import matt.obs.col.change.ListChange
import matt.obs.col.change.MultiAddAt
import matt.obs.col.change.RemoveAt
import matt.obs.col.change.RemoveAtIndices
import matt.obs.col.change.ReplaceAt

const val DEV = false

fun <E> AtomicListChange<E>.compile(): AtomicListChange<E> {

    if (isCompiled) return this

    if (DEV) taball("input changes", changes)

    val compiledChanges = mutableListOf<ListChange<E>>()


    val itr = changes.iterator()

    var lastChange: ListChange<E>? = null

    while (itr.hasNext()) {

        val c = itr.next()



        if (lastChange is RemoveAt) {
            if (c is RemoveAt) {
                if (lastChange.index == c.index) {
                    val i = c.index
                    compiledChanges.removeLast()
                    val newRemoval =
                        RemoveAtIndices(
                            collection,
                            listOf(
                                lastChange.removed.withIndex(i),
                                c.removed.withIndex(i + 1)
                            ).map { it.toKotlinIndexedValue() },
                            quickIsRange = true
                        )
                    compiledChanges += newRemoval
                    lastChange = newRemoval
                    continue
                }
            }
        } else if (lastChange is RemoveAtIndices && lastChange.isRange) {
            if (c is RemoveAt) {
                if (lastChange.firstIndex == c.index) {
                    compiledChanges.removeLast()

                    val oldSize = lastChange.removedElements.size
                    val oldItr = lastChange.removedElementsIndexed.iterator()
                    val indexOfNew = lastChange.lastIndex + 1


                    val newRemoval =
                        RemoveAtIndices(
                            collection,
                            List(oldSize + 1) {
                                if (it < oldSize) {
                                    oldItr.next().toKotlinIndexedValue()
                                } else if (it == oldSize) c.removed.withIndex(indexOfNew).toKotlinIndexedValue()
                                else NEVER
                            },

                            quickIsRange = true
                        )
                    compiledChanges += newRemoval
                    lastChange = newRemoval
                    continue
                }
            }
        } else if (lastChange is ReplaceAt) {
            val cSingleIndex =
                when (c) {
                    is RemoveAt        -> c.lowestChangedIndex
                    is RemoveAtIndices -> c.lowestChangedIndex
                    else               -> null
                }
            val cSingleRemoved =
                when (c) {
                    is RemoveAt        -> c.removed
                    is RemoveAtIndices -> c.removedElements.singleOrNull()
                    else               -> null
                }
            if (cSingleIndex != null && cSingleRemoved != null) {
                if (lastChange.index == cSingleIndex - 1) {
                    if (lastChange.added == cSingleRemoved) {
                        compiledChanges.removeLast()
                        val newRemoval =
                            RemoveAt(
                                collection,
                                lastChange.removed,
                                lastChange.index
                            )
                        compiledChanges += newRemoval
                        lastChange = newRemoval
                        continue
                    }
                }
            }
        } else if (lastChange is AddAt) {
            if (c is AddAt) {
                if (lastChange.index == c.index - 1) {
                    compiledChanges.removeLast()
                    val newAdd =
                        MultiAddAt(
                            collection = collection,
                            added = listOf(lastChange.added, c.added),
                            index = lastChange.index
                        )
                    compiledChanges += newAdd
                    lastChange = newAdd
                    continue
                }
            }
        } else if (lastChange is MultiAddAt && lastChange.isRange) {
            if (c is AddAt) {
                if (lastChange.lastIndex == c.index - 1) {
                    compiledChanges.removeLast()
                    val newAdd =
                        MultiAddAt(
                            collection = collection,
                            added = lastChange.added + listOf(c.added),
                            index = lastChange.index
                        )
                    compiledChanges += newAdd
                    lastChange = newAdd
                    continue
                }
            }
        }

        lastChange = c
        compiledChanges += c
    }

    if (DEV) taball("output changes", compiledChanges)


    return AtomicListChange(
        collection = collection,
        changes = compiledChanges,
        isCompiled = true
    )
}
