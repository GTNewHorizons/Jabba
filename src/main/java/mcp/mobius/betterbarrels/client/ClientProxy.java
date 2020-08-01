package mcp.mobius.betterbarrels.client;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;

import mcp.mobius.betterbarrels.BetterBarrels;
import mcp.mobius.betterbarrels.Utils;
import mcp.mobius.betterbarrels.bspace.BBEventHandler;
import mcp.mobius.betterbarrels.client.render.BlockBarrelRenderer;
import mcp.mobius.betterbarrels.client.render.TileEntityBarrelRenderer;
import mcp.mobius.betterbarrels.common.BaseProxy;
import mcp.mobius.betterbarrels.common.StructuralLevel;
import mcp.mobius.betterbarrels.common.blocks.TileEntityBarrel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends BaseProxy {
	public static Map<Integer, ISimpleBlockRenderingHandler> blockRenderers;

	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public void registerRenderers() {
		// Grab a static reference to the block renderers list for later use
		try {
			Field blockRendererField = RenderingRegistry.class.getDeclaredField("blockRenderers");
			blockRendererField.setAccessible(true);
			ClientProxy.blockRenderers = (Map<Integer, ISimpleBlockRenderingHandler>)blockRendererField.get(RenderingRegistry.instance());
		} catch (Throwable t) {}

		// Get the next "available" ID, and make sure it's really available
		BetterBarrels.blockBarrelRendererID = RenderingRegistry.getNextAvailableRenderId();
		while(blockRenderers.containsKey(BetterBarrels.blockBarrelRendererID)) {
			BetterBarrels.blockBarrelRendererID = RenderingRegistry.getNextAvailableRenderId();
		}

		RenderingRegistry.registerBlockHandler(BetterBarrels.blockBarrelRendererID, new BlockBarrelRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBarrel.class, new TileEntityBarrelRenderer());

		//ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMiniBarrel.class,  new TileEntityMiniBarrelRenderer());
		//ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBarrelShelf.class,  new TileEntityBarrelShelfRenderer());
		//mod_BetterBarrels.RENDER_SHELF = RenderingRegistry.getNextAvailableRenderId();
	}

	@Override
	public void checkRenderers() {
		ISimpleBlockRenderingHandler renderer = ClientProxy.blockRenderers.get(BetterBarrels.blockBarrelRendererID);

		if(!(renderer instanceof BlockBarrelRenderer)) {
			throw new RuntimeException(String.format("Wrong renderer found ! %s found while looking up the Jabba Barrel renderer.",  renderer.getClass().getCanonicalName()));
		}
	}

	@Override
	public void registerEventHandler() {
		//TODO : Turned off registering end of rendering event to check for fps drop

		MinecraftForge.EVENT_BUS.register(new BBEventHandler());
	}

	@Override
	public void postInit() {
		((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
			private boolean ranOnce = false;

			@Override
			public void onResourceManagerReload(IResourceManager resourcemanager) {
				// FML forces a ResourcePack reload after MC has finished initialising, allowing for mod icons to be registered at any point in init
				// we only want to run on the second and later reloads so that we're reasonably sure all icons have been registered
				if (!ranOnce) {
					ranOnce = true;
					return;
				}

				StructuralLevelClientData.loadBaseTextureData();
				if (StructuralLevel.LEVELS != null) {
					for (StructuralLevel level : StructuralLevel.LEVELS) {
						if (level.levelNum == 0) { continue; }
						level.clientData.generateIcons();
						StringTranslate.inject(new ByteArrayInputStream(("item.upgrade.structural." + String.valueOf(level.levelNum) + ".name=" + StatCollector.translateToLocal("item.upgrade.structural") + " " +  Utils.romanNumeral(level.levelNum) + " (" +  level.clientData.getMaterialName() + ")").getBytes(Charset.forName("UTF-8"))));
					}
				}
				StructuralLevelClientData.unloadBaseTextureData();
			}
		});
	}

	@Override
	public void initialiseClientData(int[] overrideColorData) {
		for (StructuralLevel level : StructuralLevel.LEVELS) {
			level.clientData = new StructuralLevelClientData(level);
		}

		if (overrideColorData != null) {
			if (overrideColorData.length % 2 == 0) {
				for(int i = 0; i < overrideColorData.length; i += 2) {
					if(overrideColorData[i] == 0) continue; // Can't override base barrel color...
					if(overrideColorData[i] > 0 && overrideColorData[i] < StructuralLevel.LEVELS.length) {
						StructuralLevel.LEVELS[overrideColorData[i]].clientData.setColorOverride((0xFF << 24) | overrideColorData[i+1]);
					} else {
						BetterBarrels.log.warn("Attempting to override the structural tier color for non existant tier: " + overrideColorData[i]);
					}
				}
			} else {
				BetterBarrels.log.warn("Color override list is not formatted in pairs, ignoring");
			}
		}
	}
}
