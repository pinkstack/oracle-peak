package com.pinkstack.oraclepeak

object Model {

  sealed trait Tick

  final case object Tick extends Tick

}
