package net.nethersmp.storm.voucher.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import net.nethersmp.storm.module.api.Module;
import net.nethersmp.storm.module.api.Result;
import net.nethersmp.storm.voucher.VoucherCategory;
import net.nethersmp.storm.voucher.VoucherChild;
import net.nethersmp.storm.voucher.api.VoucherCategorySettings;
import net.nethersmp.storm.voucher.api.VoucherCommand;
import net.nethersmp.storm.voucher.api.VoucherId;
import net.nethersmp.storm.voucher.api.VoucherSettings;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class VoucherLoaderModule implements Module<Void> {

    public static final String ID = "voucher_loader";
    public static final Set<String> DEPENDENCIES = Set.of();
    public static final int PRIORITY = 950;

    private final Path file;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, VoucherCategory> vouchers = new ConcurrentHashMap<>();

    private final ExecutorService loadingExecutor = Executors.newSingleThreadExecutor();

    private final Object lock = new Object();

    @Override
    public Result<Void> load() {
        AtomicReference<Result<Void>> result = new AtomicReference<>(Result.success());

        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), objectMapper.createObjectNode());
            }
            JsonNode root = objectMapper.readTree(file.toFile());

            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));

            for (String categoryId : Lists.newArrayList(root.fieldNames())) {
                JsonNode categoryNode = root.get(categoryId);
                parseCategory(categoryId, categoryNode);
            }
        } catch (IllegalArgumentException | IOException e) {
            result.set(Result.fail("[VOUCHERS] (" + e.getClass().getSimpleName() + ")", e.getMessage()));
        }

        return result.get();
    }

    @Override
    public void unload() {
        vouchers.clear();
        loadingExecutor.shutdown();
    }


    private void parseCategory(String id, JsonNode categoryData) {
        String rawMaterial = categoryData.get("material").asText().toUpperCase(Locale.getDefault());
        String color = categoryData.get("color").asText();
        String command = categoryData.get("command").asText();
        ArrayNode rawLore = categoryData.has("lore") ? (ArrayNode) categoryData.get("lore") : JsonNodeFactory.instance.arrayNode();

        ArrayNode childrenArray = (ArrayNode) categoryData.get("children");
        ConcurrentMap<String, VoucherChild<?>> children = parseChildren(childrenArray);

        List<String> lore = parseLore(rawLore);
        Material material = Material.getMaterial(rawMaterial) == null ? Material.STONE : Material.getMaterial(rawMaterial);

        VoucherCategory category = new VoucherCategory(VoucherId.of(id),
                new VoucherCategorySettings(material, command, color, lore), children);

        vouchers.put(id, category);
    }

    private ConcurrentMap<String, VoucherChild<?>> parseChildren(ArrayNode children) {
        ConcurrentMap<String, VoucherChild<?>> map = new ConcurrentHashMap<>();

        forEachRemaining(children.elements(), child -> {
            String id = child.get("id").asText();
            Object worth = parseWorth(child.get("worth"));

            VoucherSettings settings = VoucherSettings.EMPTY;
            String command = "";
            if (child.has("settings")) {
                JsonNode data = child.get("settings");

                String name = defGet(data, "name", "");
                String materialStr = defGet(data, "material", "AIR");
                String color = defGet(data, "color", "");
                command = defGet(data, "command", "");
                List<String> lore = data.has("lore") ? parseLore((ArrayNode) data.get("lore")) : new ArrayList<>();

                settings = new VoucherSettings(name, color, Material.getMaterial(materialStr), lore);
            }

            map.put(id, new VoucherChild<Object>(VoucherId.of(id), worth, settings, new VoucherCommand(command)));
        });
        return map;
    }

    private Object parseWorth(JsonNode obj) {
        if (obj.isDouble())
            return obj.asDouble();
        if (obj.isInt())
            return obj.asInt();
        if (obj.isBoolean())
            return obj.asBoolean();
        if (obj.isLong())
            return obj.asLong();
        return obj.asText();
    }

    private List<String> parseLore(ArrayNode lore) {
        List<String> result = new ArrayList<>();
        lore.forEach(node -> result.add(node.asText()));
        return result;
    }

    private String defGet(JsonNode node, String key, String def) {
        return node.has(key) ? node.get(key).asText() : def;
    }

    private <T> void forEachRemaining(Iterator<T> supplied, Consumer<T> action) {
        while (supplied.hasNext()) {
            T current = supplied.next();
            action.accept(current);
        }
    }

    public Optional<VoucherCategory> getVoucherCategory(String voucherId) {
        return Optional.ofNullable(vouchers.get(voucherId));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<String> dependencies() {
        return DEPENDENCIES;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }
}
