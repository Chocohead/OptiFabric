package me.modmuss50.optifabric.mod;

import java.io.File;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.tuple.Pair;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import org.spongepowered.asm.mixin.Mixins;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import me.modmuss50.optifabric.mod.OptifineVersion.JarType;
import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.util.RemappingUtils;

import com.chocohead.mm.api.ClassTinkerers;

public class OptifabricSetup implements Runnable {
	public static File optifineRuntimeJar = null;
	public static boolean usingScreenAPI;

	//This is called early on to allow us to get the transformers in beofore minecraft starts
	@Override
	public void run() {
		OptifineInjector injector;
		try {
			Pair<File, ClassCache> runtime = OptifineSetup.getRuntime();
			optifineRuntimeJar = runtime.getLeft();

			//Add the optifine jar to the classpath, as
			ClassTinkerers.addURL(runtime.getLeft().toURI().toURL());

			injector = new OptifineInjector(runtime.getRight());
			injector.setup();
		} catch (Throwable e) {
			if (!OptifabricError.hasError()) {
				OptifineVersion.jarType = JarType.INTERNAL_ERROR;
				OptifabricError.setError(e, "Failed to load OptiFine, please report this!\n\n" + e.getMessage());
			}
			throw new RuntimeException("Failed to setup optifine", e);
		}

		BooleanSupplier particlesPresent = new BooleanSupplier() {
			private boolean haveLooked, isPresent;

			@Override
			public boolean getAsBoolean() {
				if (!haveLooked) {
					isPresent = injector.predictFuture(RemappingUtils.getClassName("class_702")).filter(node -> {//ParticleManager
						//(MatrixStack, VertexConsumerProvider$Immediate, LightmapTextureManager, Camera, Frustum)
						String desc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597$class_4598;"
																		+ "Lnet/minecraft/class_765;Lnet/minecraft/class_4184;FLnet/minecraft/class_4604;)V");

						for (MethodNode method : node.methods) {
							if ("renderParticles".equals(method.name) && desc.equals(method.desc)) {
								return true;
							}
						}

						return false;
					}).isPresent();
					haveLooked = true;
				}

				return isPresent;
			}
		};

		if (isPresent("fabric-renderer-api-v1")) {
			Mixins.addConfiguration("optifabric.compat.fabric-renderer-api.mixins.json");
		}

		if (isPresent("fabric-rendering-v1", ">=1.5.0") && particlesPresent.getAsBoolean()) {
			Mixins.addConfiguration("optifabric.compat.fabric-rendering.mixins.json");
		}

		if (isPresent("fabric-rendering-data-attachment-v1")) {
			Mixins.addConfiguration("optifabric.compat.fabric-rendering-data.mixins.json");
		}

		if (isPresent("fabric-renderer-indigo")) {
			Mixins.addConfiguration("optifabric.compat.indigo.mixins.json");

			injector.predictFuture(RemappingUtils.getClassName("class_846$class_849")).ifPresent(node -> {//ChunkBuilder$ChunkData
				String nonEmptyLayers = RemappingUtils.mapFieldName("class_846$class_849", "field_4450", "Ljava/util/Set;");

				for (FieldNode field : node.fields) {
					if (nonEmptyLayers.equals(field.name) && "Ljava/util/Set;".equals(field.desc)) {
						return;
					}
				}

				Mixins.addConfiguration("optifabric.compat.indigo.extra-mixins.json");
			});
		}

		if (isPresent("fabric-item-api-v1", ">=1.1.0") && !isPresent("minecraft", "1.17.x")) {
			Mixins.addConfiguration("optifabric.compat.fabric-item-api.mixins.json");
		}

		if (isPresent("fabric-screen-api-v1")) {
			if (isPresent("minecraft", ">=1.17-alpha.21.10.a")) {
				Mixins.addConfiguration("optifabric.compat.fabric-screen-api.new-mixins.json");
			} else {
				Mixins.addConfiguration("optifabric.compat.fabric-screen-api.mixins.json");
			}
			usingScreenAPI = true;
		}

		Mixins.addConfiguration("optifabric.optifine.mixins.json");

		if (isPresent("cloth-client-events-v0")) {
			Mixins.addConfiguration("optifabric.compat.cloth.mixins.json");
		}

		if (isPresent("clothesline")) {
			Mixins.addConfiguration("optifabric.compat.clothesline.mixins.json");
		}

		if (isPresent("trumpet-skeleton")) {
			Mixins.addConfiguration("optifabric.compat.trumpet-skeleton.mixins.json");
		}

		if (isPresent("multiconnect", ">1.3.14")) {
			Mixins.addConfiguration("optifabric.compat.multiconnect.mixins.json");
		}

		if (isPresent("now-playing", ">=1.1.0")) {
			Mixins.addConfiguration("optifabric.compat.now-playing.mixins.json");
		}

		if (isPresent("origins", mod -> compareVersions(Pattern.compile("^1\\.16(\\.\\d)?-").matcher(mod.getVersion().getFriendlyString()).find() ? ">=1.16-0.2.0" : ">=0.4.1", mod))) {
			if (isPresent("origins", mod -> !Pattern.compile("^1\\.16(\\.\\d)?-").matcher(mod.getVersion().getFriendlyString()).find() || compareVersions(">=1.16.3-0.4.0", mod))) {
				Mixins.addConfiguration("optifabric.compat.origins.mixins.json");
			}

			injector.predictFuture(RemappingUtils.getClassName("class_979")).ifPresent(node -> {//ElytraFeatureRenderer
				String desc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_1799;Lnet/minecraft/class_1309;)Z"); //ItemStack, LivingEntity

				for (MethodNode method : node.methods) {
					if ("shouldRender".equals(method.name) && desc.equals(method.desc)) {
						Mixins.addConfiguration("optifabric.compat.origins.extra-mixins.json");
						break;
					}
				}
			});
		}

		if (isPresent("staffofbuilding")) {
			Mixins.addConfiguration("optifabric.compat.staffofbuilding.mixins.json");
		}

		if (isPresent("sandwichable", ">=1.2-alpha1")) {
			Mixins.addConfiguration("optifabric.compat.sandwichable.mixins.json");
		} else if (isPresent("sandwichable")) {
			Mixins.addConfiguration("optifabric.compat.sandwichable-old.mixins.json");
		}

		if (isPresent("astromine", "<1.6")) {//Only needed for the 1.16.1 versions
			Mixins.addConfiguration("optifabric.compat.astromine.mixins.json");
		}

		if (isPresent("carpet")) {
			Mixins.addConfiguration("optifabric.compat.carpet.mixins.json");

			if (particlesPresent.getAsBoolean()) {
				Mixins.addConfiguration("optifabric.compat.carpet.extra-mixins.json");
			}
		}

		if (isPresent("hctm-base")) {
			Mixins.addConfiguration("optifabric.compat.hctm.mixins.json");
		}

		if (isPresent("mubble", "<4.0-pre5")) {
			Mixins.addConfiguration("optifabric.compat.mubble.mixins.json");
		}

		if (isPresent("dawn", ">=1.3 <=1.4")) {
			Mixins.addConfiguration("optifabric.compat.dawn.older-mixins.json");
		} else if (isPresent("dawn", ">1.4 <1.5")) {
			Mixins.addConfiguration("optifabric.compat.dawn.old-mixins.json");
		} else if (isPresent("dawn", ">=1.5 <1.8")) {
			Mixins.addConfiguration("optifabric.compat.dawn.mixins.json");
		}

		if (isPresent("phormat")) {
			Mixins.addConfiguration("optifabric.compat.phormat.mixins.json");
		}

		if (isPresent("chat_heads", "<0.2")) {
			Mixins.addConfiguration("optifabric.compat.chat-heads.mixins.json");
		}

		if (isPresent("mmorpg")) {
			Mixins.addConfiguration("optifabric.compat.age-of-exile.mixins.json");
		}

		if (isPresent("charm", ">=2.0 <2.1")) {
			Mixins.addConfiguration("optifabric.compat.charm-old.mixins.json");
		} else if (isPresent("charm", ">=2.1")) {
			Mixins.addConfiguration("optifabric.compat.charm.mixins.json");

			if (isPresent("charm", ">=2.2.2")) {
				injector.predictFuture(RemappingUtils.getClassName("class_156")).ifPresent(node -> {//Util
					String desc = "(Lcom/mojang/datafixers/DSL$TypeReference;Ljava/lang/String;)Lcom/mojang/datafixers/types/Type;";
					String getChoiceTypeInternal = RemappingUtils.getMethodName("class_156", "method_29191", desc); //Util, getChoiceTypeInternal

					for (MethodNode method : node.methods) {
						if (getChoiceTypeInternal.equals(method.name) && desc.equals(method.desc)) {
							Mixins.addConfiguration("optifabric.compat.charm-plus.mixins.json");
							break;
						}
					}
				});
			}	
		}

		if (isPresent("voxelmap")) {
			Mixins.addConfiguration("optifabric.compat.voxelmap.mixins.json");
		}

		if (isPresent("appliedenergistics2")) {
			Mixins.addConfiguration("optifabric.compat.ae2.mixins.json");
		}

		if (isPresent("images", "=0.3.0")) {
			Mixins.addConfiguration("optifabric.compat.images-older.mixins.json");
		} else if (isPresent("images", ">=0.3.1 <1.0.1")) {
			Mixins.addConfiguration("optifabric.compat.images-old.mixins.json");
		} else if (isPresent("images", ">=1.0.1")) {
			Mixins.addConfiguration("optifabric.compat.images.mixins.json");
		}

		if (isPresent("architectury", ">=2.0.0")) {
			Mixins.addConfiguration("optifabric.compat.architectury2.mixins.json");
		} else if (isPresent("architectury", ">=1.0.20")) {
			Mixins.addConfiguration("optifabric.compat.architectury-B.mixins.json");
		} else if (isPresent("architectury", ">=1.0.4")) {
			Mixins.addConfiguration("optifabric.compat.architectury-A.mixins.json");
		}

		if (isPresent("frex", ">=4.3")) {
			Mixins.addConfiguration("optifabric.compat.frex.mixins.json");
		} else if (isPresent("frex", "=4.2")) {
			Mixins.addConfiguration("optifabric.compat.frex-old.mixins.json");
		}

		if (isPresent("full_slabs", ">=1.0.2")) {
			Mixins.addConfiguration("optifabric.compat.full-slabs.mixins.json");
		}

		if (isPresent("amecsapi", "<1.1.2")) {
			Mixins.addConfiguration("optifabric.compat.amecsapi.mixins.json");

			ClassWriter writer = new ClassWriter(0);
			writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "null", null, "java/lang/Object", null);
			writer.visitEnd(); //Just something to extend Object, only Mixin should see it
			ClassTinkerers.define("null", writer.toByteArray());
		}

		if (isPresent("pswg")) {
			Mixins.addConfiguration("optifabric.compat.pswg.mixins.json");

			if (isPresent("pswg", ">=1.16.4-0.0.15")) {
				injector.predictFuture(RemappingUtils.getClassName("class_276")).ifPresent(node -> {//FrameBuffer
					for (FieldNode field : node.fields) {
						if ("stencilEnabled".equals(field.name) && "Z".equals(field.desc)) {
							Mixins.addConfiguration("optifabric.compat.pswg.extra-mixins.json");
							break;
						}
					}
				});
			}
		}

		if (isPresent("custom-fog", ">=1.2")) {
			Mixins.addConfiguration("optifabric.compat.custom-fog.mixins.json");
		}

		if (isPresent("smooth-chunks")) {
			Mixins.addConfiguration("optifabric.compat.smooth-chunks.mixins.json");
		}

		if (isPresent("enhancedcelestials")) {
			Mixins.addConfiguration("optifabric.compat.enhancedcelestials.mixins.json");
		}

		if (isPresent("cullparticles") && particlesPresent.getAsBoolean()) {
			Mixins.addConfiguration("optifabric.compat.cullparticles.mixins.json");
		}

		if (isPresent("the_aether")) {
			Mixins.addConfiguration("optifabric.compat.aether.mixins.json");
		}

		if (isPresent("stacc")) {
			Mixins.addConfiguration("optifabric.compat.stacc.mixins.json");
		}

		if (isPresent("bannerpp")) {
			Mixins.addConfiguration("optifabric.compat.bannerpp.mixins.json");
		}
	}

	private static boolean isPresent(String modID) {
		return FabricLoader.getInstance().isModLoaded(modID);
	}

	private static boolean isPresent(String modID, String versionRange) {
		return isPresent(modID, modMetadata -> compareVersions(versionRange, modMetadata));
	}

	private static boolean isPresent(String modID, Predicate<ModMetadata> extraChecks) {
		if (!isPresent(modID)) return false;

		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(modID);
		ModMetadata modMetadata = modContainer.map(ModContainer::getMetadata).orElseThrow(() ->
			new RuntimeException("Failed to get mod container for " + modID + ", something has broke badly.")
		);

		return extraChecks.test(modMetadata);
	}

	private static boolean compareVersions(String versionRange, ModMetadata mod) {
		try {
			Predicate<SemanticVersionImpl> predicate = SemanticVersionPredicateParser.create(versionRange);
			SemanticVersionImpl version = new SemanticVersionImpl(mod.getVersion().getFriendlyString(), false);
			return predicate.test(version);
		} catch (@SuppressWarnings("deprecation") net.fabricmc.loader.util.version.VersionParsingException e) {
			System.err.println("Error comparing the version for ".concat(MoreObjects.firstNonNull(mod.getName(), mod.getId())));
			e.printStackTrace();
			return false; //Let's just gamble on the version not being valid also not being a problem
		}
	}
}
