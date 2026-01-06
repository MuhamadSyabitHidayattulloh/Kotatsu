@file:JvmName("InjektKt")

package uy.kohesive.injekt

import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.InjektFactory
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

val Injekt: InjektScope = InjektScopeImpl

object InjektScopeImpl : InjektScope, InjektFactory {

	private val instances = mutableMapOf<Class<*>, Any>()

	init {
		instances[NetworkHelper::class.java] = NetworkHelper()
		instances[Json::class.java] = Json {
			ignoreUnknownKeys = true
			isLenient = true
			coerceInputValues = true
			explicitNulls = false
		}
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getInstance(forClass: Class<T>): T {
		return instances[forClass] as? T
			?: throw IllegalStateException("No instance registered for ${forClass.name}")
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getInstance(forType: Type): T {
		val clazz = when (forType) {
			is Class<*> -> forType
			is ParameterizedType -> forType.rawType as Class<*>
			else -> throw IllegalArgumentException("Unsupported type: $forType")
		}
		return instances[clazz] as? T
			?: throw IllegalStateException("No instance registered for $forType")
	}

	fun <T : Any> addSingleton(forType: Class<T>, instance: T) {
		instances[forType] = instance
	}

	inline fun <reified T : Any> get(): T = getInstance(T::class.java)
}

inline fun <reified T : Any> injectLazy(): Lazy<T> = lazy { InjektScopeImpl.get() }
