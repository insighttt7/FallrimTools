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

public class StagePatcher {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: StagePatcher <input.ess> <formID_hex> <output.ess> <stage1,stage2,...>");
            System.out.println("Example: StagePatcher save.ess D9B64 output.ess 10,20,30,200");
            return;
        }

        Path inputPath = Paths.get(args[0]);
        int formID = (int) Long.parseLong(args[1], 16);
        Path outputPath = Paths.get(args[2]);
        String[] stageParts = args[3].split(",");

        short[][] stages = new short[stageParts.length][2];
        for (int i = 0; i < stageParts.length; i++) {
            stages[i][0] = Short.parseShort(stageParts[i].trim());
            stages[i][1] = 1; // status: executed
        }

        ModelBuilder model = new ModelBuilder(new ProgressModel());
        ESS.Result result = ESS.readESS(inputPath, model);
        ESS ess = result.ESS;

        RefID refID = ess.getContext().makeRefID(formID);
        ChangeForm form = ess.getChangeForms().getChangeForm(refID);

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

        Flags.Int newFlags = form.getChangeFlags().with(31); // CHANGE_QUEST_STAGES bit
        form.updateRawData(qust, newFlags);

        ESS.writeESS(ess, outputPath, false);
        System.out.println("Done. Wrote " + outputPath);
    }
}
