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
            stages[i][0] = Short.parseShort(stageParts[i].trim());
            stages[i][1] = 1; // status: executed
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

        int existingFlags = form.getChangeFlags().FLAGS;
int newFlagsValue = existingFlags | (1 << 31) | (1 << 26);
Flags.Int newFlags = new Flags.Int(newFlagsValue); // CHANGE_QUEST_STAGES bit
        form.updateRawData(qust, newFlags);

// --- Create MQ302FillAliases ChangeForm from scratch ---
int fillAliasesRaw = 0x4876E6;

ByteBuffer headerBuf = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
headerBuf.put((byte) ((fillAliasesRaw >> 16) & 0xFF));
headerBuf.put((byte) ((fillAliasesRaw >> 8) & 0xFF));
headerBuf.put((byte) (fillAliasesRaw & 0xFF));
headerBuf.putInt(0);
headerBuf.put((byte) 8);
headerBuf.put((byte) 78);
headerBuf.put((byte) 0);
headerBuf.put((byte) 0);
((java.nio.Buffer) headerBuf).flip();

ChangeForm fillForm = new ChangeForm(headerBuf, ess.getContext());

ByteBuffer bodyBuf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);
bodyBuf.putShort((short) 0x1100);
bodyBuf.put((byte) 0x04);
bodyBuf.putShort((short) 10);
bodyBuf.put((byte) 1);
bodyBuf.put((byte) 1);
((java.nio.Buffer) bodyBuf).flip();

Flags.Int fillFlags = new Flags.Int((1 << 1) | (1 << 26) | (1 << 31));
ChangeFormQust fillQust = new ChangeFormQust(bodyBuf, fillFlags, ess.getContext());

fillForm.updateRawData(fillQust, fillFlags);
ess.getChangeForms().add(fillForm);
// --- end MQ302FillAliases ---

ESS.writeESS(ess, outputPath, false);
System.out.println("Done. Wrote " + outputPath);
    }
}
