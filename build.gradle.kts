import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
}

group = "io.fatsan.fac"
version = "1.0.0"
description = "Fatsan Anti Cheat (FAC) - Folia-first anti-cheat scaffold"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
  maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
  paperweight.foliaDevBundle("1.21.11-R0.1-SNAPSHOT")
  compileOnly("com.github.retrooper:packetevents-spigot:2.11.2")
  testImplementation(platform("org.junit:junit-bom:5.10.2"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
  compileJava { options.release = 21 }
  test { useJUnitPlatform() }
  javadoc { options.encoding = Charsets.UTF_8.name() }
}

bukkitPluginYaml {
  name = "FAC_Folia"
  main = "io.fatsan.fac.bootstrap.FatsanAntiCheatPlugin"
  load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
  apiVersion = "1.21"
  foliaSupported = true
  authors.add("Fatsan")
  website = "https://example.invalid/fac"
  description = project.description
  prefix = "FAC"
  commands.register("fac") {
    description = "FAC admin command"
    usage = "/fac <status|premium|feedback|label|dq|reload>"
    permission = "fac.admin"
  }
  permissions.register("fac.admin") {
    description = "FAC admin command access"
  }
  permissions.register("fac.alerts") {
    description = "Receive FAC violation alert notifications"
  }
  permissions.register("fac.bypass") {
    description = "Exempt from FAC anti-cheat detection (staff/trusted players)"
  }
}
