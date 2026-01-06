package uy.kohesive.injekt.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

interface InjektScope {
	fun <T : Any> getInstance(forClass: Class<T>): T
}

interface InjektFactory {
	fun <T : Any> getInstance(forClass: Class<T>): T
	fun <T : Any> getInstance(forType: java.lang.reflect.Type): T
}

interface InjektModule

interface InjektRegistrar {
	fun <T : Any> addSingleton(forType: Class<T>, instance: T)
	fun <T : Any> addSingletonFactory(forType: Class<T>, factory: () -> T)
	fun <T : Any> addFactory(forType: Class<T>, factory: () -> T)
	fun <T : Any, R : T> addAlias(forType: Class<T>, toType: Class<R>)
}

interface TypeReference<T> {
	val type: Type
}

abstract class FullTypeReference<T> protected constructor() : TypeReference<T> {
	override val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
}
