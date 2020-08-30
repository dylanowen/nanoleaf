package com.dylowen.house

import com.dylowen.house.utils.NotNothing
import org.mockito.ArgumentMatchers.argThat
import org.mockito.stubbing.Stubber
import org.mockito.{ArgumentCaptor, ArgumentMatcher, ArgumentMatchers, Mockito}
import org.scalatestplus.mockito.MockitoSugar

import scala.reflect.ClassTag

/**
  * @author dylan.owen
  * @since Aug-2020
  */
trait MockitoSpec extends MockitoSugar {

  def doReturn(toBeReturned: Any, other: Any*): Stubber = {
    Mockito.doReturn(toBeReturned, other.map(_.asInstanceOf[Object]): _*)
  }

  def doCallRealMethod(): Stubber = {
    Mockito.doCallRealMethod()
  }

  def any[T <: AnyRef: NotNothing](implicit classTag: ClassTag[T]): T = {
    ArgumentMatchers.any(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  def eqTo[T <: Any](toBeEvaluated: T): T = {
    ArgumentMatchers.eq(toBeEvaluated)
  }

  def matchFunc[T](f: T => Boolean): T = {
    argThat(((arg: T) => f(arg)): ArgumentMatcher[T])
  }

  def capture[T: NotNothing](implicit classTag: ClassTag[T]): ArgumentCaptor[T] = {
    ArgumentCaptor.forClass[T, T](classTag.runtimeClass.asInstanceOf[Class[T]])
  }
}
