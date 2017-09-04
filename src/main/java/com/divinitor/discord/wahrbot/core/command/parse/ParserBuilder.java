package com.divinitor.discord.wahrbot.core.command.parse;

import java.util.function.*;

public class ParserBuilder {

    private ParserBuilder() {}

    public static ParserBuilder builder() {
        return new ParserBuilder();
    }

    public ParserBuilder prefix(String prefix) {

        return this;
    }

    public ParserBuilder literal(String literal) {

        return this;
    }

    public <T extends Enum> EnumChoiceParserBuilder<T> switchEnum(Class<T> choices) {
        return new EnumChoiceParserBuilder<>(this, choices);
    }

    public ParserBuilder stringParam(String name) {

        return this;
    }

    public ParserBuilder stringParam(String name, Predicate<String> validator) {

        return this;
    }

    public ParserBuilder intParam(String name) {

        return this;
    }

    public <T> ParserBuilder customParam(String name, Function<String, T> convert) {

        return this;
    }

    public ParserBuilder intParam(String name, IntPredicate validator) {

        return this;
    }

    public ParserBuilder userReferenceParam(String name) {

        return this;
    }

    public ParserBuilder channelReferenceParam(String name) {

        return this;
    }

    public ParserBuilder roleReferenceParam(String name) {

        return this;
    }

    public ParserBuilder remainder() {

        return this;
    }

    public Parser build() {
        return null;
    }

    public ParserBuilder call(Consumer<Parser> handler) {

        return this;
    }

    public static class EnumChoiceParserBuilder<T extends Enum> {

        private final ParserBuilder parent;
        private final Class<T> choices;

        public EnumChoiceParserBuilder(ParserBuilder parent, Class<T> choices) {
            this.parent = parent;
            this.choices = choices;
        }

        public EnumChoiceParserBuilder<T> option(T t, UnaryOperator<ParserBuilder> child) {

            return this;
        }

        public EnumChoiceParserBuilder<T> defaultOption(UnaryOperator<ParserBuilder> child) {

            return this;
        }

        public ParserBuilder endSwitch() {
            return this.parent;
        }
    }

    enum MinificCommands {
        ADD,
        DELETE,
        EDIT
    }

    class Example {
        public void example() {
            ParserBuilder.builder()
                .prefix(".")
                .literal("minific")
                .switchEnum(MinificCommands.class)
                .option(MinificCommands.ADD, b -> b
                    .stringParam("minificName")
                    .remainder()
                    .call(this::add)
                )
                .option(MinificCommands.DELETE, b -> b
                    .stringParam("minificName")
                    .call(this::delete)
                )
                .option(MinificCommands.EDIT, b -> b
                    .stringParam("minificName")
                    .remainder()
                    .call(this::edit)
                )
                .defaultOption(b -> b
                    .call(this::badCommand)
                )
                .endSwitch()
                .build();
        }

        public void badCommand(Parser parser) {

        }

        public void add(Parser input) {

        }

        public void delete(Parser input) {

        }

        public void edit(Parser input) {

        }

    }
}
