package io.github.aeckar.kanary

import io.github.aeckar.kanary.utils.jvmName
import kotlin.reflect.KClass

/**
 * The scope wherein a protocol's [read] and [write] operations are defined.
 */
class ProtocolBuilder<T : Any>(internal val classRef: KClass<*>) {
    private var _read: TypedReadOperation<T>? = null
    private var _write: TypedWriteOperation<T>? = null

    init {
        if (classRef in TypeFlag.K_CLASSES) {
            throw MalformedProtocolException(classRef, "built-in protocol already exists")
        }
        if (classRef.jvmName == null) {
            throw MalformedProtocolException(classRef, "local and anonymous classes cannot be serialized")
        }
    }

    /**
     * The binary read operation called when [Deserializer.read] is called with an object of class [T].
     * Information deserialized from supertypes is converted into packets of information,
     * from which the read operation can use the information to create a new instance of [T].
     * @throws MalformedProtocolException [T] is an abstract and the 'fallback' modifier is not passed,
     * or is read from,
     * or is assigned to more than once in a single scope
     */
    var read: TypedReadOperation<T>
        @Deprecated("Reading from this property throws MalformedProtocolException")
        get() = throw MalformedProtocolException(classRef, "read operation may only be set, not read")
        set(value) {
            if (classRef.isAbstract && value !is FallbackReadOperation) {
                throw MalformedProtocolException(classRef,
                    "read operation without 'fallback' modifier not supported for abstract classes and interfaces")
            }
            _read?.let {
                throw MalformedProtocolException(classRef, "read operation assigned a value more than once")
            }
            _read = value
        }

    /**
     * The binary write operation called when [Serializer.write] is called with an object of class [T].
     * @throws MalformedProtocolException is read from,
     * or is assigned to more than once in a single scope
     */
    var write: TypedWriteOperation<T>
        @Deprecated("Reading from this property throws MalformedProtocolException")
        get() = throw MalformedProtocolException(classRef, "read operation may only be set, not read")
        set(value) {
            _write?.let {
                throw MalformedProtocolException(classRef, "write operation assigned a value more than once")
            }
            _write = value
        }

    /**
     * When prepended to a [read operation][read], declares that subtypes without a read operation
     * can still be instantiated as an instance of [T] using this read operation.
     * Generally, this should be used for types whose subtypes have the same public API.
     * Any information not deserialized as a result is lost.
     * @throws MalformedProtocolException [T] is a final class,
     * or called more than once in a single scope
     */
    fun fallback(read: TypedReadOperation<T>): TypedReadOperation<T> {
        if (classRef.isFinal) {
            throw MalformedProtocolException(classRef, "'fallback' modifier not supported for final classes")
        }
        return FallbackReadOperation(read)
    }

    /**
     * When prepended to a [write operation][write], declares that the only information serialized
     * from an instance of [T] is that which is specifically written here.
     * If used, subtypes of this type may not define a protocol with a write operation.
     * Enables certain optimizations.
     * @throws MalformedProtocolException this function is called more than once in a single scope
     */
    fun static(write: TypedWriteOperation<T>): TypedWriteOperation<T> {
        return StaticWriteOperation(write)
    }

    @PublishedApi
    internal fun readOrNull() = _read as ReadOperation?

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun writeOrNull() = _write as WriteOperation?
}