package com.gu.nitf.model


abstract class IdValuedEnumeration(initial: Int = 0) extends Enumeration(initial) {
  class Val extends super.Val { override def toString: String = id.toString }
}

abstract class LowerCaseEnumeration extends Enumeration {
  class Val extends super.Val { override def toString: String = super.toString.toLowerCase }
}

object ManagementStatus extends LowerCaseEnumeration {
  val Usable, Embargoed, Withheld, Canceled = new Val
}

object MediaType extends LowerCaseEnumeration {
  val Text, Audio, Image, Video, Data, Application, Other = new Val
}

object NewsUrgency extends IdValuedEnumeration(1) {
  val One, Two, Three, Four, Five, Six, Seven, Eight = new Val
  val Most = One
  val Normal = Five
  val Least = Eight
}
