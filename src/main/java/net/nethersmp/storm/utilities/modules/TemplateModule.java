package net.nethersmp.storm.utilities.modules;

import net.nethersmp.storm.module.api.Module;
import net.nethersmp.storm.module.api.Result;

import java.util.Set;

public class TemplateModule implements Module<Void> {

    public static final String ID = "";
    public static final Set<String> DEPENDENCIES = Set.of();
    public static final int PRIORITY = 1000;

    @Override
    public Result<Void> load() {
        return Result.success();
    }

    @Override
    public void unload() {

    }


    @Override
    public String id() {
        return ID;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Set<String> dependencies() {
        return DEPENDENCIES;
    }
}
