import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.sap.translation.core.doc.ITMgrDocument;
import com.sap.translation.core.engine.IEntityInfo;
import com.sap.translation.core.engine.ITMgrEngine;
import com.sap.translation.core.file.FileDocumentFactory;
import com.sap.translation.core.file.TMgrMergeReport;
import com.sap.translation.core.xliff.IXliffExporter;
import com.sap.translation.core.xliff.IXliffImporter;
import com.sap.translation.sdk.TranslationSDKManager;
import com.sap.translation.sdk.TranslatableEntity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TmtBridge {

    public static void main(String[] args) {
        IEnterpriseSession session = null;
        try {
            String action = getArg(args, "--action");
            String cms    = getArg(args, "--cms");
            String user   = getArg(args, "--user");
            String pass   = getArg(args, "--pass");
            String cuid   = getArg(args, "--cuid");
            String file   = getArg(args, "--file");  // dossier pour export et import
            String source = getArg(args, "--source");

            if (action == null || cms == null || user == null || pass == null) {
                System.err.println("Arguments manquants");
                System.exit(1);
            }

            // --- LOGON ---
            session = CrystalEnterprise.getSessionMgr()
                    .logon(user, pass, cms, "secEnterprise");
            session.setLocale(Locale.ENGLISH);

            // --- ACTION : TEST ---
            if ("test".equals(action)) {
                System.out.println("OK:login");
                return;
            }

            if (cuid == null || file == null) {
                System.err.println("--cuid et --file requis");
                System.exit(1);
            }

            // --- RECUPERER L'INFOOBJECT ---
            IInfoStore infostore = (IInfoStore) session.getService("InfoStore");
            String query = "select * from ci_infoobjects where SI_CUID='" + cuid + "'";
            IInfoObjects objects = (IInfoObjects) infostore.query(query);
            if (objects.isEmpty()) {
                query = "select * from ci_appobjects where SI_CUID='" + cuid + "'";
                objects = (IInfoObjects) infostore.query(query);
            }
            if (objects.isEmpty()) {
                System.err.println("ERREUR: objet introuvable pour CUID : " + cuid);
                System.exit(1);
            }
            IInfoObject infoObj = (IInfoObject) objects.get(0);
            System.out.println("Objet trouvé : " + infoObj.getTitle());

            // --- EXTRAIRE LES TRADUCTIONS ---
            TranslationSDKManager manager = new TranslationSDKManager(session);
            InputStream translations = manager.extractTranslations(infoObj.getCUID());

            File tmgrTemp = File.createTempFile("tmgr_", ".tmgr");
            tmgrTemp.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(tmgrTemp)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = translations.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            ITMgrDocument tmgrdoc = manager.loadArtifact(
                    TranslatableEntity.TRANSMGR, tmgrTemp);
            ITMgrEngine engine = ITMgrEngine.Factory.createInstance(tmgrdoc);

            // --- ACTION : EXPORT ---
            if ("export".equals(action)) {
                if (source == null) {
                    System.err.println("--source requis pour l'export");
                    System.exit(1);
                }

                // Créer le dossier de sortie si nécessaire
                File outputDir = new File(file);
                if (!outputDir.exists()) outputDir.mkdirs();

                Locale sourceLocale = parseLocale(source);
                Locale[] availableLocales = engine.getAvailableLocales();
                System.out.println("Locales disponibles :");
                for (Locale loc : availableLocales) {
                    System.out.println("  - " + loc);
                }

                for (Locale loc : availableLocales) {
                    if (!loc.equals(sourceLocale)) {
                        engine.setLocaleVisible(loc, true);
                        engine.setModifiedLocale(loc);
                    }
                }

                IEntityInfo rootEntity = engine.getEntityInfo(engine.getRoot());

                // Un fichier XLIFF par locale cible
                List<Locale> targets = new ArrayList<>();
                for (Locale loc : availableLocales) {
                    if (!loc.equals(sourceLocale)) {
                        targets.add(loc);
                    }
                }

                for (Locale targetLocale : targets) {
                    IXliffExporter exporter = TranslationSDKManager.createXliffExporter();
                    exporter.setSourceLocale(sourceLocale, true);
                    exporter.setTargetLocale(targetLocale);

                    File outFile = new File(outputDir, targetLocale.toString() + ".xliff");
                    exporter.write(rootEntity, outFile);
                    System.out.println("OK:export:" + outFile.getPath());
                }

                // --- ACTION : IMPORT ---
            } else if ("import".equals(action)) {

                // Scanner le dossier pour trouver tous les fichiers .xliff
                File importDir = new File(file);
                File[] xliffFiles = importDir.listFiles(
                        (d, name) -> name.toLowerCase().endsWith(".xliff"));

                if (xliffFiles == null || xliffFiles.length == 0) {
                    System.err.println("Aucun fichier .xliff trouvé dans : " + file);
                    System.exit(1);
                }

                // 1. Configurer toutes les locales comme visibles et modifiables
                Locale[] availableLocales = engine.getAvailableLocales();
                for (Locale loc : availableLocales) {
                    engine.setLocaleVisible(loc, true);
                    engine.setModifiedLocale(loc);
                }

                // 2. save() AVANT les loads — pattern officiel SDK
                engine.save();

                // 3. Charger chaque XLIFF dans le même engine
                IXliffImporter importer = TranslationSDKManager.createXliffImporter();
                for (File xliffFile : xliffFiles) {
                    System.out.println("Import : " + xliffFile.getName());
                    importer.load(engine, xliffFile);
                }

                // 4. Sérialiser l'engine vers un flux tmgr
                ByteArrayOutputStream tmgrStream = new ByteArrayOutputStream();
                FileDocumentFactory.exportToTMgr(engine, tmgrStream);

                // 5. Envoyer le tmgr complet vers BO (toutes les langues d'un coup)
                InputStream saveResult = manager.saveTranslations(
                        infoObj.getCUID(), tmgrStream, true);

                // 6. Vérifier les conflits
                TMgrMergeReport mergeResult = new TMgrMergeReport(saveResult);
                if (mergeResult.hasConflict()) {
                    System.err.println("CONFLIT: les traductions ont des conflits");
                    System.exit(2);
                }

                System.out.println("OK:import:" + xliffFiles.length + " fichier(s) importé(s)");

            } else {
                System.err.println("Action inconnue : " + action);
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("ERREUR:" + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (session != null) {
                try { session.logoff(); } catch (Exception ignored) {}
            }
        }
    }

    private static String getArg(String[] args, String key) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) return args[i + 1];
        }
        return null;
    }

    private static Locale parseLocale(String localeStr) {
        if (localeStr.contains("_")) {
            String[] parts = localeStr.split("_");
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(localeStr);
    }
}