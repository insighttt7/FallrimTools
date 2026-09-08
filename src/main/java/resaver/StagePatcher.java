package resaver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import resaver.ess.ESS;
import resaver.ess.ChangeForm;
import resaver.ess.ChangeFormData;
import resaver.ess.ChangeFormQust;
import resaver.ess.Flags;
import resaver.ess.RefID;
import resaver.ess.ModelBuilder;
import resaver.ProgressModel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class StagePatcher {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
    System.out.println("Usage: StagePatcher <input.ess> --list");
    System.out.println("   or: StagePatcher <input.ess> <formID_hex> <output.ess> <stage1,stage2,...>");
    return;
}
Path inputPath = Paths.get(args[0]);

ModelBuilder modelEarly = new ModelBuilder(new ProgressModel());
ESS.Result resultEarly = ESS.readESS(inputPath, modelEarly);
ESS essEarly = resultEarly.ESS;

if (args.length >= 2 && args[1].equals("--list")) {
    for (ChangeForm cf : essEarly.getChangeForms()) {
        if (cf.getType() == ChangeForm.Type.QUST) {
            System.out.println("raw=" + cf.getRefID().toRaw()
                    + " hex=" + cf.getRefID().toHex()
                    + " formid=" + Integer.toHexString(cf.getRefID().FORMID)
                    + " str=" + cf.getRefID().toString());
        }
    }
    return;
}

int formID = (int) Long.parseLong(args[1], 16);
Path outputPath = Paths.get(args[2]);
String[] stageParts = args[3].split(",");
        short[][] stages = new short[stageParts.length][2];
        for (int i = 0; i < stageParts.length; i++) {
    String part = stageParts[i].trim();
    if (part.contains(":")) {
        String[] pair = part.split(":");
        stages[i][0] = Short.parseShort(pair[0].trim());
        stages[i][1] = Short.parseShort(pair[1].trim());
    } else {
        stages[i][0] = Short.parseShort(part);
        stages[i][1] = 1;
    }
}

        ESS ess = essEarly;

       ChangeForm form = null;
for (ChangeForm cf : ess.getChangeForms()) {
    if (cf.getRefID().equals(formID)) {
        form = cf;
        break;
    }
}

        if (form == null) {
            System.out.println("ChangeForm not found for formID " + args[1] + " - is the quest running in this save?");
            return;
        }

        ChangeFormData data = form.getData(Optional.empty(), ess.getContext(), false);
        if (!(data instanceof ChangeFormQust)) {
            System.out.println("Form is not a QUST changeform.");
            return;
        }

        ChangeFormQust qust = (ChangeFormQust) data;
        qust.setStagesDirect(stages, true);
       if (args.length >= 5) {
    short flagsValue = (short) Integer.parseInt(args[4], 16);
    qust.setQuestFlagsDirect(flagsValue);
}
if (args.length >= 7 && !args[6].isEmpty()) {
    String[] objParts = args[6].split(",");
    int[][] objectives = new int[objParts.length][2];
    for (int i = 0; i < objParts.length; i++) {
        String[] pair = objParts[i].split(":");
        objectives[i][0] = Integer.parseInt(pair[0].trim());
        objectives[i][1] = Integer.parseInt(pair[1].trim());
    }
    qust.setObjectivesDirect(objectives);
} else {
    qust.setObjectivesEmpty();
}

        int existingFlags = form.getChangeFlags().FLAGS;
int newFlagsValue = existingFlags | (1 << 31) | (1 << 26) | (1 << 29);
Flags.Int newFlags = new Flags.Int(newFlagsValue); // CHANGE_QUEST_STAGES bit
        form.updateRawData(qust, newFlags);
ESS.writeESS(ess, outputPath, false);
System.out.println("Done. Wrote " + outputPath);
    }
}
