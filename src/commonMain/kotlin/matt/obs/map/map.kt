package matt.obs.map

import matt.lang.anno.Open
import matt.lang.common.ILLEGAL
import matt.lang.weak.common.WeakRefInter
import matt.log.warn.common.warn
import matt.model.flowlogic.keypass.KeyPass
import matt.obs.common.MListenable
import matt.obs.common.MObservableImpl
import matt.obs.listen.MapListener
import matt.obs.listen.MyListenerInter
import matt.obs.listen.update.MapUpdate
import matt.obs.map.change.Clear
import matt.obs.map.change.ItrRemove
import matt.obs.map.change.MapChange
import matt.obs.map.change.Put
import matt.obs.map.change.PutAll
import matt.obs.map.change.Remove
import matt.obs.map.change.RemoveValue
import matt.obs.prop.ValProp
import matt.obs.prop.writable.VarProp
import kotlin.collections.MutableMap.MutableEntry

interface BasicOMap<K, V> : Map<K, V>, MListenable<MapListener<K, V>> {
    @Open
    override fun observe(op: () -> Unit) = onChange { op() }

    @Open
    override fun observeWeakly(
        w: WeakRefInter<*>,
        op: () -> Unit
    ): MyListenerInter<*> {
        TODO()
    }

    fun onChange(op: (MapChange<K, V>) -> Unit): MapListener<K, V>
}

abstract class InternallyBackedOMap<K, V> internal constructor(map: Map<K, V>) :
    MObservableImpl<MapUpdate<K, V>, MapListener<K, V>>(), BasicOMap<K, V>, Map<K, V> by map {



        companion object {
            private var didWarn = false
        }

        final override fun onChange(op: (MapChange<K, V>) -> Unit): MapListener<K, V> =
            addListener(
                MapListener {
                    op(it)
                }
            )

        internal val bindWritePass = KeyPass()
        protected fun emitChange(change: MapChange<K, V>) {
            if (!didWarn) {
                warn(
                    """
                    	I guess InternallyBackedOMap needs similar infrastructural support as ObsList? like:
                    require(this !is BindableList<*> || !this.isBound || bindWritePass.isHeld)  
                    """.trimIndent()
                )
                didWarn = true
            }

            notifyListeners(MapUpdate((change)))
        }

        val isEmptyProp: ValProp<Boolean> by lazy {
            VarProp(isEmpty()).apply {
                onChange {
                    value = this@InternallyBackedOMap.isEmpty()
                }
            }
        }
    }

interface BasicOMutableMap<K, V> : BasicOMap<K, V>, MutableMap<K, V>

class BasicOMutableMapImpl<K, V>(private val map: MutableMap<K, V> = mutableMapOf()) :
    InternallyBackedOMap<K, V>(map),
    BasicOMutableMap<K, V> {


    override val entries: MutableSet<MutableEntry<K, V>> =
        object : MutableSet<MutableEntry<K, V>> {

            inner class ObsMutableEntry(entry: MutableEntry<K, V>) : MutableEntry<K, V> {
                override val key: K = entry.key
                override val value: V = entry.value

                override fun setValue(newValue: V): V {
                    put(key, newValue)
                    return value
                }
            }

            override fun add(element: MutableEntry<K, V>): Boolean {
                if (contains(element)) return false
                put(element.key, element.value)
                return true
            }

            override fun addAll(elements: Collection<MutableEntry<K, V>>): Boolean {
                val rs =
                    elements.map {
                        add(it)
                    }
                return rs.any()
            }

            override val size: Int
                get() = map.entries.size

            override fun clear() {
                this@BasicOMutableMapImpl.clear()
            }

            override fun isEmpty(): Boolean = this@BasicOMutableMapImpl.isEmpty()

            override fun containsAll(elements: Collection<MutableEntry<K, V>>): Boolean = elements.all { contains(it) }

            override fun contains(element: MutableEntry<K, V>) = (get(element.key) == element.value)

            override fun iterator(): MutableIterator<MutableEntry<K, V>> =
                object : MutableIterator<MutableEntry<K, V>> {
                    private val itr = map.entries.iterator()
                    override fun hasNext(): Boolean = itr.hasNext()

                    override fun next(): MutableEntry<K, V> = ObsMutableEntry(itr.next())

                    override fun remove() {
                        itr.remove()
                        emitChange(ItrRemove(map))
                    }
                }

            override fun retainAll(elements: Collection<MutableEntry<K, V>>): Boolean {
                var r = false
                val itr = iterator()
                while (itr.hasNext()) {
                    val next = itr.next()
                    if (elements.none {
                            it.key == next.key && it.value == next.value
                        }
                    ) {
                        r = true
                        itr.remove()
                    }
                }
                return r
            }

            override fun removeAll(elements: Collection<MutableEntry<K, V>>): Boolean {
                val rs = elements.map { remove(it) }
                return rs.any()
            }

            override fun remove(element: MutableEntry<K, V>): Boolean {
                if (!contains(element)) return false
                this@BasicOMutableMapImpl.remove(element.key)
                return true
            }
        }
    override val keys: MutableSet<K> =
        object : MutableSet<K> {
            override fun add(element: K): Boolean {
                ILLEGAL
            }

            override fun addAll(elements: Collection<K>): Boolean {
                ILLEGAL
            }

            override val size: Int
                get() = map.keys.size

            override fun clear() {
                this@BasicOMutableMapImpl.clear()
            }

            override fun isEmpty(): Boolean = map.keys.isEmpty()

            override fun containsAll(elements: Collection<K>): Boolean = elements.all { contains(it) }

            override fun contains(element: K): Boolean = map.keys.contains(element)

            override fun iterator(): MutableIterator<K> =
                object : MutableIterator<K> {
                    private val itr = map.keys.iterator()
                    override fun hasNext(): Boolean = itr.hasNext()

                    override fun next(): K = itr.next()

                    override fun remove() {
                        itr.remove()
                        emitChange(ItrRemove(map))
                    }
                }

            override fun retainAll(elements: Collection<K>): Boolean {
                var r = false
                val itr = iterator()
                while (itr.hasNext()) {
                    val next = itr.next()
                    if (elements.none {
                            it == next
                        }
                    ) {
                        r = true
                        itr.remove()
                    }
                }
                return r
            }

            override fun removeAll(elements: Collection<K>): Boolean {
                val rs = elements.map { remove(it) }
                return rs.any()
            }

            override fun remove(element: K): Boolean {
                if (!contains(element)) return false
                this@BasicOMutableMapImpl.remove(element)
                return true
            }
        }
    override val values: MutableSet<V> =
        object : MutableSet<V> {
            override fun add(element: V): Boolean {
                ILLEGAL
            }

            override fun addAll(elements: Collection<V>): Boolean {
                ILLEGAL
            }

            override val size: Int
                get() = map.values.size

            override fun clear() {
                this@BasicOMutableMapImpl.clear()
            }

            override fun isEmpty(): Boolean = map.values.isEmpty()

            override fun containsAll(elements: Collection<V>): Boolean = elements.all { contains(it) }

            override fun contains(element: V): Boolean = map.values.contains(element)

            override fun iterator(): MutableIterator<V> =
                object : MutableIterator<V> {
                    private val itr = map.values.iterator()
                    override fun hasNext(): Boolean = itr.hasNext()

                    override fun next(): V = itr.next()

                    override fun remove() {
                        itr.remove()
                        emitChange(ItrRemove(map))
                    }
                }

            override fun retainAll(elements: Collection<V>): Boolean {
                var r = false
                val itr = iterator()
                while (itr.hasNext()) {
                    val next = itr.next()
                    if (elements.none {
                            it == next
                        }
                    ) {
                        r = true
                        itr.remove()
                    }
                }
                return r
            }

            override fun removeAll(elements: Collection<V>): Boolean {
                val rs = elements.map { remove(it) }
                return rs.any()
            }

            override fun remove(element: V): Boolean {
                val r = map.values.remove(element)
                if (r) {
                    emitChange(RemoveValue(this@BasicOMutableMapImpl))
                }
                return r
            }
        }

    override fun clear() {
        map.clear()
        emitChange(Clear(this))
    }

    override fun put(
        key: K,
        value: V
    ): V? {
        val r = map.put(key, value)
        emitChange(Put(this, key, value))
        return r
    }

    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
        emitChange(PutAll(this, from = from))
    }

    override fun remove(key: K): V? {
        val r = map.remove(key)
        emitChange(Remove(this, key))
        return r
    }
}

