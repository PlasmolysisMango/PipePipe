package org.pipepipe.plugin.tools;

import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.schabi.newpipe.extractor.plugin.PluginManifest;
import org.schabi.newpipe.extractor.plugin.PluginPackage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates the plugin signing key and writes the signed {@code index.json} that
 * PipePipe's plugin repository settings consume.
 *
 * <pre>
 * PluginTool genkey &lt;key-file&gt;
 * PluginTool index &lt;key-file&gt; &lt;repo-dir&gt; &lt;repo-name&gt;
 * </pre>
 */
public final class PluginTool {

    private static final int SEED_SIZE = 32;

    private PluginTool() {
    }

    public static void main(final String[] args) throws Exception {
        if (args.length == 2 && "genkey".equals(args[0])) {
            genkey(new File(args[1]));
            return;
        }
        if (args.length == 4 && "index".equals(args[0])) {
            index(new File(args[1]), new File(args[2]), args[3]);
            return;
        }
        System.err.println("Usage:");
        System.err.println("  PluginTool genkey <key-file>");
        System.err.println("  PluginTool index <key-file> <repo-dir> <repo-name>");
        System.exit(2);
    }

    private static void genkey(final File keyFile) throws IOException {
        final Ed25519PrivateKeyParameters privateKey =
                new Ed25519PrivateKeyParameters(new SecureRandom());
        final byte[] seed = privateKey.getEncoded();
        writeSecret(keyFile.toPath(), Base64.encode(seed));
        final String publicKey = Base64.toBase64String(privateKey.generatePublicKey().getEncoded());
        System.out.println("Wrote Ed25519 signing key to " + keyFile.getAbsolutePath());
        System.out.println("Public key (embedded in index.json): " + publicKey);
        System.out.println("Back this file up and add its content as the PLUGIN_SIGNING_KEY secret.");
    }

    private static void writeSecret(final Path path, final byte[] encoded) throws IOException {
        Files.write(path, encoded, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            final Set<PosixFilePermission> ownerOnly =
                    PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, ownerOnly);
        } catch (final UnsupportedOperationException | IOException ignored) {
            // non-POSIX file system, nothing to tighten
        }
    }

    private static void index(final File keyFile, final File repoDir, final String repoName)
            throws Exception {
        final Ed25519PrivateKeyParameters privateKey = readKey(keyFile);
        final String publicKey = Base64.toBase64String(
                privateKey.generatePublicKey().getEncoded());

        final File[] packages = repoDir.listFiles(
                (dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(PluginPackage.FILE_EXTENSION));
        if (packages == null || packages.length == 0) {
            throw new IOException("No .ppp packages in " + repoDir.getAbsolutePath());
        }
        Arrays.sort(packages, Comparator.comparing(File::getName));

        final List<Entry> entries = new ArrayList<>();
        for (final File file : packages) {
            final PluginManifest manifest = PluginPackage.readManifest(file);
            if (!PluginPackage.hasDex(file)) {
                throw new IOException(file.getName() + " has no " + PluginPackage.DEX_ENTRY);
            }
            if (manifest.getIcon() != null) {
                PluginPackage.readIcon(file, manifest);
            }
            final byte[] digest = PluginPackage.sha256(file);
            entries.add(new Entry(file, manifest, Hex.toHexString(digest),
                    sign(privateKey, digest)));
        }

        final JsonStringWriter writer = JsonWriter.string().object()
                .value("formatVersion", 1)
                .value("name", repoName)
                .array("plugins");
        for (final Entry entry : entries) {
            final PluginManifest manifest = entry.manifest;
            writer.object()
                    .value("pluginId", manifest.getPluginId())
                    .value("name", manifest.getName())
                    .value("versionCode", manifest.getVersionCode())
                    .value("versionName", manifest.getVersionName())
                    .value("apiVersion", manifest.getApiVersion())
                    .value("author", manifest.getAuthor())
                    .value("package", entry.file.getName())
                    .value("sha256", entry.sha256)
                    .value("signature", entry.signature)
                    .value("publicKey", publicKey);
            if (manifest.getDescription() != null) {
                writer.value("description", manifest.getDescription());
            }
            writer.value("nsfw", manifest.isNsfw())
                    .end();
            System.out.println("Indexed " + entry.file.getName());
        }
        final String json = writer.end().end().done();

        final Path out = new File(repoDir, "index.json").toPath();
        Files.write(out, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + entries.size() + " plugin(s) to " + out.toAbsolutePath());
    }

    private static Ed25519PrivateKeyParameters readKey(final File keyFile) throws IOException {
        final String encoded = new String(Files.readAllBytes(keyFile.toPath()),
                StandardCharsets.US_ASCII).replaceAll("\\s", "");
        final byte[] seed;
        try {
            seed = Base64.decode(encoded);
        } catch (final RuntimeException e) {
            throw new IOException("Signing key is not valid base64", e);
        }
        if (seed.length != SEED_SIZE) {
            throw new IOException("Signing key must be " + SEED_SIZE
                    + " raw Ed25519 bytes, got " + seed.length);
        }
        return new Ed25519PrivateKeyParameters(seed, 0);
    }

    private static String sign(final Ed25519PrivateKeyParameters key, final byte[] message) {
        final Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, key);
        signer.update(message, 0, message.length);
        return Base64.toBase64String(signer.generateSignature());
    }

    private static final class Entry {
        private final File file;
        private final PluginManifest manifest;
        private final String sha256;
        private final String signature;

        private Entry(final File file, final PluginManifest manifest, final String sha256,
                      final String signature) {
            this.file = file;
            this.manifest = manifest;
            this.sha256 = sha256;
            this.signature = signature;
        }
    }
}
