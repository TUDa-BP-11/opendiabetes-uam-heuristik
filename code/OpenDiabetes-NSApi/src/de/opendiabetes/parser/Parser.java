package de.opendiabetes.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public interface Parser<T> {
    T parse(String input);

    default T parseFile(String path) {
        return parseFile(Paths.get(path));
    }

    default T parseFile(Path path) {
        StringBuilder builder = new StringBuilder();

        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.forEach(builder::append);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return parse(builder.toString());
    }
}
