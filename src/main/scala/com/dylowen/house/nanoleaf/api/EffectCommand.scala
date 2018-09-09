package com.dylowen.house.nanoleaf.api

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object ExplodeEffect {
  def apply(command: String = "",
            duration: Option[Int] = None,
            palette: Seq[Palette],
            transTime: Range,
            delayTime: Range,
            explodeFactor: Double = 0.5,
            direction: String = "outwards"): EffectCommand = {
    EffectCommand(
      command = command,
      duration = duration,
      animType = "explode",
      colorType = Some("HSB"),
      palette = palette,
      transTime = Some(transTime),
      delayTime = Some(delayTime),
      explodeFactor = Some(explodeFactor),
      direction = Some(direction)
    )
  }
}

object RandomEffect {
  def apply(command: String = "",
            duration: Option[Int] = None,
            palette: Seq[Palette],
            transTime: Range,
            delayTime: Range,
            brightnessRange: Range): EffectCommand = {
    EffectCommand(
      command = command,
      duration = duration,
      animType = "random",
      colorType = Some("HSB"),
      palette = palette,
      transTime = Some(transTime),
      delayTime = Some(delayTime),
      brightnessRange = Some(brightnessRange)
    )
  }
}

final case class EffectCommand(command: String,
                               version: String = "1.0",
                               duration: Option[Int] = None,
                               animType: String,
                               colorType: Option[String] = None,
                               palette: Seq[Palette] = Seq(),
                               transTime: Option[Range] = None,
                               delayTime: Option[Range] = None,
                               brightnessRange: Option[Range] = None,
                               explodeFactor: Option[Double] = None,
                               direction: Option[String] = None)

final case class Palette(hue: Int, saturation: Int, brightness: Int)

final case class Range(minValue: Int, maxValue: Int)

case class WriteWrapper(write: EffectCommand)