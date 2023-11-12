package me.Thelnfamous1.dual_diamond_sword;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(DualDiamondSword.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DualDiamondSword {
    public static final String MODID = "dual_diamond_sword";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Item> DUAL_DIAMOND_SWORD = ITEMS.register("dual_diamond_sword", () ->
            new BoomerangSwordItem(Tiers.DIAMOND, 3, -2.4F, (new Item.Properties()).tab(CreativeModeTab.TAB_COMBAT)));

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<EntityType<BoomerangSwordEntity>> BOOMERANG_SWORD = ENTITY_TYPES.register("boomerang_sword", () ->
            EntityType.Builder.<BoomerangSwordEntity>of(BoomerangSwordEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build(new ResourceLocation(MODID, "boomerang_sword").toString()));

    public DualDiamondSword() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Do something when the setup is run on both client and server
        LOGGER.info("HELLO from common setup!");
    }

    @SubscribeEvent
    static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(BOOMERANG_SWORD.get(), BoomerangSwordRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Do something when the setup is run on only the client
        LOGGER.info("HELLO from client setup!");
    }
}
