plugins {
    id("com.alkeari.modtrix.fabric")
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.shedaniel.me/") }
        filter { includeGroupAndSubgroups("me.shedaniel") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.terraformersmc.com/releases/") }
        filter { includeGroup("com.terraformersmc") }
    }
}

dependencies {
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${modtrix.dep("cloth_config")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    // Only the ModMenuApi interface is compiled against; Mod Menu's own dependencies are never needed.
    modCompileOnly("com.terraformersmc:modmenu:${modtrix.dep("modmenu")}") {
        isTransitive = false
    }
}
