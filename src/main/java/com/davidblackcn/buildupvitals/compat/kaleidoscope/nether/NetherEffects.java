package com.davidblackcn.buildupvitals.compat.kaleidoscope.nether;
import com.davidblackcn.buildupvitals.compat.kaleidoscope.common.CuisineEffectAdapter;
import net.minecraft.resources.Identifier;
public final class NetherEffects {
    private NetherEffects() { }
    public static void register() {
        for (String name : new String[]{"star_blessing","crimson","ghost","warped","tropical_strider","mysterious_poison"})
            CuisineEffectAdapter.register(Identifier.parse("kaleidoscope_nether:"+name),new CuisineEffectAdapter.Semantics(false,false,false,name.equals("star_blessing"),false));
    }
}
