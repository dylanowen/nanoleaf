package com.dylowen.house.nanoleaf

import com.dylowen.house.Seq
import com.dylowen.house.nanoleaf.api.{EffectCommand, Palette, RandomEffect, Range}

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object DefinedEffect {
  val Default: EffectCommand = RandomEffect(
    command = "displayTemp",
    duration = Some(10),
    palette = Seq(
      Palette(0, 0, 10),
      Palette(0, 0, 20),
      Palette(0, 0, 30),
      Palette(0, 0, 40),
    ),
    transTime = Range(3, 10),
    delayTime = Range(3, 7),
    brightnessRange = Range(50, 100)
  )

  val Effects: Map[String, EffectCommand] = Map(
    "default" -> Default,
    "bluegreen-random" -> RandomEffect(
      palette = Seq(
        Palette(221, 74, 80),
        Palette(181, 71, 80),
        Palette(163, 43, 80),
        Palette(198, 64, 80),
        Palette(221, 82, 55),
        Palette(128, 71, 55),
        Palette(150, 90, 55),
      ),
      transTime = Range(3, 10),
      delayTime = Range(3, 7),
      brightnessRange = Range(50, 100)
    ),
    "purple-random" -> RandomEffect(
      palette = Seq(
        Palette(300, 75, 80),
        Palette(285, 71, 80),
        Palette(264, 67, 80),
        Palette(246, 65, 80),
      ),
      transTime = Range(3, 10),
      delayTime = Range(3, 7),
      brightnessRange = Range(50, 100)
    ),
    "fire-random" -> RandomEffect(
      palette = Seq(
        Palette(25, 84, 90),
        Palette(55, 80, 90),
        Palette(13, 89, 90),
      ),
      transTime = Range(3, 10),
      delayTime = Range(3, 7),
      brightnessRange = Range(50, 100)
    )
  )

  def apply(key: String): EffectCommand = {
    Effects.getOrElse(key, Default)
  }
}
