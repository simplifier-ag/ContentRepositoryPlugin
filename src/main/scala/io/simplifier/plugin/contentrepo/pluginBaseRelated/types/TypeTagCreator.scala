package io.simplifier.plugin.contentrepo.pluginBaseRelated.types

import scala.reflect.ClassTag
import scala.reflect.api.TypeCreator
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{currentMirror, universe}


/**
 * Trait that provided method for creation of type tags.
 */
trait TypeTagCreator {

  import TypeTagCreator._

  /**
   * Creates a <b>TypeTag</b> from a provided object.
   *
   * @param `object`    the object to get the type tag from.
   * @param mirror      the runtime mirror.
   * @tparam T          the type parameter [[T]].
   *
   * @return            the TypeTag[[T]].
   */
  def typeTagFromObject[T: ClassTag](`object`: T,
                                     mirror: reflect.api.Mirror[reflect.runtime.universe.type] = currentMirror): TypeTag[T] = {
    mirror match {
      case null => throw MirrorMayNotBeNull()
      case `object` if `object` == null => TypeTag(mirror, createTypeCreator(TypeTag.Null.tpe, mirror))
      case _ => createTypeTag(`object`)
    }
  }

  /**
   * Creates a <b>TypeTag</b> from a provided class.
   *
   * @param clazz     the class to get the type tag from.
   * @param mirror    the runtime mirror.
   * @tparam T        the type parameter [[T]].
   *
   * @return          the TypeTag[[T]].
   */
  def typeTagFromClass[T: ClassTag](clazz: Class[T],
                                    mirror: reflect.api.Mirror[reflect.runtime.universe.type] = currentMirror): TypeTag[T] = {
    mirror match {
      case null => throw MirrorMayNotBeNull()
      case _ if clazz == null => TypeTag(mirror, createTypeCreator(TypeTag.Null.tpe, mirror))
      case _ => createTypeTag(clazz)
    }
  }

  /**
   * Creates a <b>TypeTag</b> from a provided Type.
   *
   * @param tpe       the object to get the type tag from.
   * @param mirror    the runtime mirror.
   * @tparam T        the type parameter [[T]].
   *
   * @return          the TypeTag[[T]].
   */
  def typeTagFromType[T](tpe: Type,
                         mirror: reflect.api.Mirror[reflect.runtime.universe.type] = currentMirror): TypeTag[T] = {
    if (mirror == null) throw MirrorMayNotBeNull() else TypeTag(mirror, createTypeCreator(tpe, mirror))
  }


  /**
   * Creates a class from a provided <b>TypeTag</b>
   *
   * @tparam T    the TypeTag[[T]].
   *
   * @return      the respective class
   */
  def getClassFromTag[T: TypeTag]: Class[_] = {
    val mirror: universe.Mirror = runtimeMirror(getClass.getClassLoader)
    mirror.runtimeClass(typeOf[T].typeSymbol.asClass)
  }


  private[this] def createTypeTag[T: ClassTag](`object`: T): TypeTag[T] = {
    val clazz: Class[_ <: T] = `object`.getClass
    createTypeTag(clazz)
  }


  private[this] def createTypeTag[T: ClassTag](clazz: Class[_ <: T]): TypeTag[T] = {
    val mirror: universe.Mirror = runtimeMirror(clazz.getClassLoader)
    val symbol: universe.ClassSymbol = mirror.staticClass(clazz.getName)
    val tpe: universe.Type = symbol.selfType
    TypeTag(mirror, createTypeCreator(tpe, mirror))
  }


  private[this] def createTypeCreator(tpe: Type,
                                      mirror: reflect.api.Mirror[reflect.runtime.universe.type]): TypeCreator = {
    mirror match {
      case null => throw MirrorMayNotBeNull()
      case _ => new TypeCreator {
        def apply[U <: reflect.api.Universe with Singleton](mir: reflect.api.Mirror[U]): U#Type = {
          mir match {
            case null => throw MirrorMayNotBeNull(REFLECTION_MIRROR)
            case m if m != mirror => throw TpeNotMigratable(tpe, mirror, m)
            case _ => tpe match {
              case null => TypeTag.Null.asInstanceOf[U#Type]
              case _ => tpe.asInstanceOf[U#Type]
            }
          }
        }
      }
    }
  }
}


/** The Type Tag Creator Companion Object */
object TypeTagCreator {

  private[TypeTagCreator] val REFLECTION_MIRROR: String = "reflection api"


  private[TypeTagCreator] def MirrorMayNotBeNull(mirrorType: String = "universe"): IllegalArgumentException =
    new IllegalArgumentException(s"The provided $mirrorType mirror reference may not be null.", new NullPointerException("mirror"))


  private[TypeTagCreator] def TpeNotMigratable(tpe: Type,
                                               mirror: reflect.api.Mirror[reflect.runtime.universe.type],
                                               m: reflect.api.Mirror[_]): IllegalStateException =
    new IllegalStateException(s"The TypeTag[$tpe] defined in the mirror: [$mirror] cannot be migrated to the mirror: [$m].")
}