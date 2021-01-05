# OptiFabric

![](https://ss.modmuss50.me/javaw_2019-05-22_20-33-34.jpg)

__Note:__ This project is not related or supported by either Fabric or Optifine.

__Note:__ This project does not contain Optifine, you must download it separately!

## Installing

After installing fabric for 1.16.3, you will need to place the OptiFabric mod jar as well as the optifine installer in the mods folder.

Fabric Loader should be the latest version from the [Fabric Website](https://fabricmc.net/use/)

If you need more help you can read a more detailed guide [here](https://github.com/modmuss50/OptiFabric/wiki/Install-Tutorial)


## Links

### [OptiFabric Downloads](https://minecraft.curseforge.com/projects/optifabric)

### [Optifine Download](https://optifine.net/downloads)

## Issues

If you happen to find an issue and you believe it is to do with OptiFabric and not Optifine or a mod please open an issue [here](https://github.com/Chocohead/OptiFabric/issues) 


## For Mod Devs

Add the following to your build.gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // replace OptiFabric:<version> with latest version on https://www.curseforge.com/minecraft/mc-mods/optifabric/files that fits your MC version
    modImplementation 'com.github.Chocohead:OptiFabric:<version>'
} 
```

Put the standard Optifine jar in /run/mods

Class export can be enabled using the following VM Option, this will extract the overwritten classes to the .optifine folder, useful for debugging.

`-Doptifabric.extract=true`

## Screenshots

Feel free to open a PR with more screenshots.

![](https://ss.modmuss50.me/javaw_2019-05-22_20-36-25.jpg)

![](https://ss.modmuss50.me/javaw_2019-05-22_19-49-41.jpg)

## How it works

This would not have been possible without Chocohead's [Fabric-ASM](https://github.com/Chocohead/Fabric-ASM).

1. The mod looks for an optifine installer or mod jar in the current mods folder
2. If it finds an installer jar it runs the extract task in its own throwaway classloader.
3. The optifine mod jar is a set of classes that need to replace the ones that minecraft provides.
4. Optifine's replacement classes change the name of some lambada methods, so I take a good guess at the old name (using the original minecraft jar).
5. Remap optifine to intermediary (or yarn in development)
6. Move the patched classes out as they wont do much good on the classpath twice
7. Add optifine to the classpath
8. Register the patching tweaker for every class that needs replacing
9. Replace the target class with the class that was extracted, also do some more fixes to it, and make it public (due to access issues).
10. Hope it works
