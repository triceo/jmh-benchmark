package org.example;

import org.drools.modelcompiler.ExecutableModelProject;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.conf.PropertySpecificOption;
import org.kie.internal.event.rule.RuleEventManager;
import org.kie.internal.utils.KieHelper;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.impl.score.buildin.hardsoft.HardSoftScoreHolderImpl;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.drools.OptaPlannerRuleEventListener;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;

final class DroolsSession implements Session {

    private static final String DRL = "import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScoreHolder;\n" +
            "import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;\n" +
            "import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;\n" +
            "global HardSoftScoreHolder scoreHolder;\n" +
            "rule \"requiredCpuPowerTotal\"\n" +
            "    when\n" +
            "        $computer : CloudComputer($cpuPower : cpuPower)\n" +
            "        accumulate(\n" +
            "            CloudProcess(\n" +
            "                computer == $computer,\n" +
            "                $requiredCpuPower : requiredCpuPower);\n" +
            "            $requiredCpuPowerTotal : sum($requiredCpuPower);\n" +
            "            $requiredCpuPowerTotal > $cpuPower\n" +
            "        )\n" +
            "    then\n" +
            "        scoreHolder.addHardConstraintMatch(kcontext, $cpuPower - $requiredCpuPowerTotal);\n" +
            "end\n";
    private static final DroolsScoreDirectorFactory<CloudBalance, ?> SDF =
            new DroolsScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, buildKieBase());
    private final KieSession session;
    private final HardSoftScoreHolderImpl scoreHolder;

    public DroolsSession() {
        session = SDF.newKieSession();
        ((RuleEventManager) session).addEventListener(new OptaPlannerRuleEventListener());
        scoreHolder = new HardSoftScoreHolderImpl(false);
        SDF.getRuleToConstraintWeightExtractorMap().forEach((rule, extractor) -> {
            HardSoftScore constraintWeight = (HardSoftScore) extractor.apply(MyBenchmark.FULL_SOLUTION);
            MyBenchmark.SOLUTION_DESCRIPTOR.validateConstraintWeight(rule.getPackageName(), rule.getName(), constraintWeight);
            scoreHolder.configureConstraintWeight(rule, constraintWeight);
        });
        session.setGlobal("scoreHolder", scoreHolder);
    }

    private static KieBase buildKieBase() {
        return new KieHelper(PropertySpecificOption.DISABLED)
                .addContent(DRL, ResourceType.DRL)
                .build(ExecutableModelProject.class);
    }

    @Override
    public int insert(Object object) {
        session.insert(object);
        return 0;
    }

    @Override
    public HardSoftScore calculateScore() {
        session.fireAllRules();
        return scoreHolder.extractScore(0);
    }

    @Override
    public void close() {
        session.dispose();
    }
}
