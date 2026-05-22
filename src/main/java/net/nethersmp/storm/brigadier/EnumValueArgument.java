package net.nethersmp.storm.brigadier;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class EnumValueArgument<E extends Enum<?>> implements CustomArgumentType.Converted<E, String> {

    private static final DynamicCommandExceptionType ERROR_INVALID_FIELD = new DynamicCommandExceptionType(field -> {
        return MessageComponentSerializer.message().serialize(Component.text(field + " is not a valid constant."));
    });

    private final Class<?> enumClass;
    private final boolean suggestLowercase;

    @Override
    public E convert(String s) throws CommandSyntaxException {
        try {
            if (!enumClass.isEnum()) return null;

            for (Object enumConstant : enumClass.getEnumConstants()) {
                if (((Enum<?>) enumConstant).name().equalsIgnoreCase(s))
                    return (E) enumConstant;
            }

        } catch (IllegalArgumentException ignored) {
            throw ERROR_INVALID_FIELD.create(getNativeType());
        }
        return null;
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, @NonNull SuggestionsBuilder builder) {
        for (Object enumConstant : enumClass.getEnumConstants()) {
            String constantName = ((Enum<?>) enumConstant).name();

            if (suggestLowercase)
                constantName = constantName.toLowerCase();

            if (constantName.startsWith(builder.getRemainingLowerCase()))
                builder.suggest(constantName);

        }
        return builder.buildFuture();
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
