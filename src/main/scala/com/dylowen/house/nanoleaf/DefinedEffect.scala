package com.dylowen.house.nanoleaf

import com.dylowen.house.Seq
import com.dylowen.house.nanoleaf.api._

/**
  * TODO add description
  *
  * @author dylan.owen
  * @since Sep-2018
  */
object DefinedEffect {

  val SolarizedAccentPalette: Seq[Palette] = Seq(
    Palette(45, 100, 71), // yellow
    Palette(18, 89, 80), // orange
    Palette(1, 79, 86), // red
    Palette(331, 74, 83), // magenta
    Palette(237, 45, 77), // violet
    Palette(205, 82, 82), // blue
    Palette(175, 74, 63), // cyan
    Palette(68, 100, 60), // green
  )

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

  val ManualModeOn: EffectCommand = FadeEffect(
    command = "displayTemp",
    duration = Some(2),
    palette = Seq(
      Palette(100, 95, 75),
      Palette(100, 95, 10),
    ),
    transTime = Range(5, 5),
    delayTime = Range(1, 1),
  )

  val ManualModeOff: EffectCommand = FadeEffect(
    command = "displayTemp",
    duration = Some(2),
    palette = Seq(
      Palette(9, 93, 75),
      Palette(9, 93, 10),
    ),
    transTime = Range(5, 5),
    delayTime = Range(1, 1),
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
    "fire-random" -> RandomEffect(
      palette = Seq(
        Palette(25, 84, 90),
        Palette(55, 80, 90),
        Palette(13, 89, 90),
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
    "solarized-dark-random" -> RandomEffect(
      palette = Seq(
        Palette(193, 100, 21), // base03
        Palette(192, 90, 26), // base02
        Palette(194, 25, 46), // base01
        Palette(195, 23, 51), // base00
      ) ++ SolarizedAccentPalette,
      transTime = Range(5, 7),
      delayTime = Range(10, 20),
      brightnessRange = Range(50, 100)
    ),
    "solarized-light-random" -> RandomEffect(
      palette = Seq(
        Palette(186, 13, 59), // base0
        Palette(180, 9, 63), // base1
        Palette(44, 11, 93), // base2
        Palette(44, 10, 99), // base3
      ) ++ SolarizedAccentPalette,
      transTime = Range(5, 7),
      delayTime = Range(10, 20),
      brightnessRange = Range(50, 100)
    ),
    "sweater-shirt" -> RandomEffect(
      palette = Seq(
        Palette(0, 0, 31),
        Palette(205, 13, 77),
        Palette(9, 68, 60),
        Palette(23, 59, 84),
        Palette(34, 24, 95),
      ),
      transTime = Range(5, 7),
      delayTime = Range(10, 20),
      brightnessRange = Range(50, 100)
    ),
  )

  def apply(key: String): EffectCommand = {
    Effects.getOrElse(key, Default)
  }
}
