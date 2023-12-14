1.20.x Changelog
48.1
====
 - 48.1.0 1.20.2 RB 1 - https://forums.minecraftforge.net/topic/139824-forge-481-minecraft-1202/

48.0
====
 - 48.0.49 Implemented ClientPauseEvent/Hook (#9782)
 - 48.0.48 Datagen addOptionalTag/s methods that allow passing the TagKey itself instead of passing the location (#9807)
 - 48.0.47 Deprecate EntityRenderersEvent.AddLayers functions that hard case to LivingEntityRenderers as the backing maps do not guarantee that type. Closes #9683
 - 48.0.46 Fix TagLoader error not printing tag name correctly. Closes #9693
 - 48.0.45 Fix LoadingErrorScreen inner headers are not centered. Closes #9687
 - 48.0.44 Rework KeyModifiers system to properly allow keybinds to be triggered when multiple modifiers are pressed.
           Fix setting keybinds whel using keyboard inputs to select the menu. Closes #9793
 - 48.0.43 Fix KeyModifiers not being properly taken into account. Closes #9806
 - 48.0.42 Don't turn off VSync when rendering from Minecraft context (#9800)
           Co-authored-by: embeddedt <42941056+embeddedt@users.noreply.github.com>
 - 48.0.41 [1.20.x] Fix rare crash with early display window, fixes #9673 (#9798)
           Co-Authored-By: embeddedt <42941056+embeddedt@users.noreply.github.com>
 - 48.0.40 Move Chunk Watch/Unwatch events to fire after sync packets have been sent to the client (#9790)
           This makes sure that modders who send extra chunk data will have clients who know about the chunk.
 - 48.0.39 Add more sanitization to the ChannelListManager because other projects like to impose arbitrary critical restrictions. Closes #9789 #9772
 - 48.0.38 Fix tag loading being unordered. Closes #9774
 - 48.0.37 Fix texture UV values for obj models. Closes #9737 (#9767)
 - 48.0.36 Gradle 8.4 (#9778)
 - 48.0.35 Make Ingredient check for invalidation on getItems(), invalidate ingredients on tags update (#9688)
 - 48.0.34 Improve mod description formatting in mods screen (#9768)
 - 48.0.33 Fix lowcode implementation version, now that it's actually labeling the correct package.
 - 48.0.32 Buildscript cleanup and organization.
           Updated to use newer libraries that reduce the command line arguments needed to run the game.
           Fix issues in the SecureModule's classloader that caused resources to not be seen when they are on parent module layers.
 - 48.0.31 Fix @GameTestGenerator not functioning correctly. Closes #9748
 - 48.0.30 Optimise registries by using fastutils collections to minimize boxing.  (#9749)
 - 48.0.29 Minor MDK changes (#9750)
 - 48.0.28 Improve mod loading error message for errors inside mod constructors (#9751)
 - 48.0.27 Add a CrashReportAnalyser that tries to identify the mod that crashed the game (#9732)
 - 48.0.26 Fix potential threading issue when registering CriteriaTriggers. Closes #9745
 - 48.0.25 Move to a cache based crowdin solution instead of hitting the api every build.
 - 48.0.24 Fix Melon/Pumpkin stems having wrong plat type causing them to remain after trampling farmland. Fixes #9495
 - 48.0.23 Fix loot tables not having their ids set.
 - 48.0.22 Make horses and llama's fire LivingFallEvent. Fixes #9743
 - 48.0.21 Fix advancement screen being too dark. Fixes #9727
 - 48.0.20 Fix ConditionalCodec having an inverted test. Closes #9742
           Remove clean project on CI.
           Bump FG version to fix transitive issue on module path.
 - 48.0.19 More buildscript work
           Fixed ignoreList for client and server.
           Lazy configured a few more tasks
           Fixed early loading screen hardcoded names.. why?
 - 48.0.18 Bump guava to module path for jimfs. Fixes dedicated server.
 - 48.0.17 Revive custom ingredient types.
 - 48.0.16 Revive ConditionalRecipe and ConditionalAdvancement.
           Also directly add conditional support to all vanilla recipe data generators.
 - 48.0.15 Rewrite our GameTest hooks to be sane.
           Add game test to GlobalLootModifierTest which validates the smelting modifiers.
           Removed FakePlayer system, it's broken and needs a major rewrite.
 - 48.0.14 Cleanup FMLLoader and Launch Handlers.
           Cleanup some of the build.gradle
           Start work on restructuring test mods. They are now treated as their own jars, as well as having their mods.toml auto-generated. No more mods.toml conflicts!
           Add pattern matching support to Data run modlist.
 - 48.0.13 Fix Slot Index for Inventory Ticking Items (#9736)
 - 48.0.12 Make default Entity.getAddPacket automatically use Forge's enhanced spawn packet if nessasary.
           This spawn packet needs to be re-designed, it is effectively only there to hook into creating the new entity instance. This will be re-evaluated during the cleanup in 1.21+
 - 48.0.11 Optimise path filter in mod loading (#9710)
           Co-authored-by: embeddedt <42941056+embeddedt@users.noreply.github.com>
 - 48.0.10 Fix ignitedByLava making blocks permanently flammable (#9735) Fixes #9730
 - 48.0.9  Some BackgroundScanHandler code cleanup. Closes #9713
 - 48.0.8  Fix missed patch causing new onInventoryTick method to not be called.
 - 48.0.7  Fixed PlayerDestroyItemEvent not triggering for Fishing Rods (#9729)
 - 48.0.6  Fix migration error causing nether and end portals to not function properly.
 - 48.0.5  Optimise `ForgeRegistry#validateContent` by removing optimization that ModLauncher broke. (#9715)
 - 48.0.4  Fix double message encoding in `Channel#toVanillaPacket` #9721 (#9722)
           Fix OpenContainer message not resetting reader index of additional data.
 - 48.0.3  Make CriteriaTriggers#register public again. Closes #9723
 - 48.0.2  Fix Hunger bar rendering incorrectly. Fixes #9725
 - 48.0.1  Send known channels to the server when they send us theirs.
 - 48.0.0  1.20.2 Release

47.999
======
 - 47.999.12 Enable ForgeLootTableProvider.
 - 47.999.11 Global Loot Modifiers
 - 47.999.10 Move old tests.
 - 47.999.9  1.20.2-rc2
 - 47.999.8  1.20.2-rc1
 - 47.999.7  1.20.2-pre4
 - 47.999.6  1.20.2-pre3
 - 47.999.5  Use official mappings for in-repo patches, will be converted to SRG during build.
 - 47.999.4  1.20.2-pre2
 - 47.999.3  Fix debug overlay text not rendering.
 - 47.999.2  Make SimpleChannel use a varInt for the discriminator allowing essentially unlimited packets.
 - 47.999.1  Rewrite networking.
 - 47.999.0  1.20.2-pre1

