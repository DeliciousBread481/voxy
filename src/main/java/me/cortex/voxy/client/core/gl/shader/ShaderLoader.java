package me.cortex.voxy.client.core.gl.shader;


import net.caffeinemc.mods.sodium.client.gl.shader.ShaderConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderLoader {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^\\s*#import\\s+<([^>]+)>\\s*$");

    public static String parse(String id) {
        return "#version 460 core\n" + stripVersionDirective(resolveImports(id, new HashSet<>()));
    }

    private static String resolveImports(String id, Set<String> seen) {
        if (!seen.add(id)) {
            throw new RuntimeException("Circular shader import detected: " + id);
        }

        String source = loadShaderSource(id).replace("\r\n", "\n");
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String importedId = matcher.group(1);
            String importedSource = stripVersionDirective(resolveImports(importedId, seen));
            matcher.appendReplacement(result, Matcher.quoteReplacement(importedSource));
        }
        matcher.appendTail(result);
        seen.remove(id);
        return result.toString();
    }

    private static String loadShaderSource(String id) {
        String[] split = id.split(":", 2);
        if (split.length != 2) {
            throw new RuntimeException("Invalid shader identifier: " + id);
        }

        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(split[0], "shaders/" + split[1]);
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            try {
                var resource = client.getResourceManager().getResource(resourceLocation);
                if (resource.isPresent()) {
                    try (InputStream stream = resource.get().open()) {
                        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            } catch (IOException ignored) {
                // Fall through to classpath lookup for environments where the resource manager is not ready yet.
            }
        }

        String classpathPath = "assets/" + split[0] + "/shaders/" + split[1];
        try (InputStream stream = ShaderLoader.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading shader: " + id, e);
        }

        throw new RuntimeException("Shader not found: " + id);
    }

    private static String stripVersionDirective(String source) {
        return source.replaceFirst("(?m)^\\s*#version\\s+.+\\R", "");
    }
}
