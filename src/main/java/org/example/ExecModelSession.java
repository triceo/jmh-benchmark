package org.example;

import org.drools.model.Drools;
import org.drools.model.Global;
import org.drools.model.Model;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.builder.KieBaseBuilder;
import org.drools.modelcompiler.dsl.pattern.D;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.event.rule.RuleEventManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScoreHolder;
import org.optaplanner.core.impl.score.buildin.hardsoft.HardSoftScoreHolderImpl;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.drools.OptaPlannerRuleEventListener;
import org.optaplanner.examples.cloudbalancing.domain.CloudBalance;
import org.optaplanner.examples.cloudbalancing.domain.CloudComputer;
import org.optaplanner.examples.cloudbalancing.domain.CloudProcess;

final class ExecModelSession implements Session {

    public static Model requiredCpuPowerTotal_GroupBy() {
        final Global<HardSoftScoreHolder> var_scoreHolder = D.globalOf( HardSoftScoreHolder.class, "" );
        final Variable<CloudComputer> var_$computer = D.declarationOf(CloudComputer.class);
        final Variable<Integer> var_$cpuPower = D.declarationOf(Integer.class, "$cpuPower");
        final Variable<CloudProcess> var_$process = D.declarationOf(CloudProcess.class);
        final Variable<Integer> var_$requiredCpuPower = D.declarationOf(Integer.class, "$requiredCpuPower");
        final Variable<Integer> var_$requiredCpuPowerTotal = D.declarationOf(Integer.class);

        org.drools.model.Rule rule = D.rule("requiredCpuPowerTotal")
                .build(
                        D.groupBy(
                                D.pattern(var_$process).bind(var_$requiredCpuPower, ( CloudProcess _this) -> _this.getRequiredCpuPower()),
                                var_$process, var_$computer, CloudProcess::getComputer,
                                D.accFunction(org.drools.core.base.accumulators.IntegerSumAccumulateFunction::new, var_$requiredCpuPower).as(var_$requiredCpuPowerTotal)
                        ),
                        D.pattern(var_$requiredCpuPowerTotal).expr("b", var_$cpuPower, ( Integer $requiredCpuPowerTotal, Integer $cpuPower) -> org.drools.modelcompiler.util.EvaluationUtil.greaterThanNumbers($requiredCpuPowerTotal, $cpuPower)),
                        D.on(var_$cpuPower, var_$requiredCpuPowerTotal, var_scoreHolder).execute(( Drools drools, Integer $cpuPower, Integer $requiredCpuPowerTotal, HardSoftScoreHolder scoreHolder) -> {
                            {
                                scoreHolder.addHardConstraintMatch((org.kie.api.runtime.rule.RuleContext) drools, $cpuPower - $requiredCpuPowerTotal);
                            }
                        }));
        final ModelImpl model = new ModelImpl();
        model.addGlobal(var_scoreHolder);
        model.addRule(rule);
        return model;
    }
    private static final DroolsScoreDirectorFactory<CloudBalance, ?> SDF =
            new DroolsScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, buildKieBase());
    private final KieSession session;
    private final HardSoftScoreHolderImpl scoreHolder;

    public ExecModelSession() {
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
        return KieBaseBuilder.createKieBaseFromModel(requiredCpuPowerTotal_GroupBy());
    }

    @Override
    public int insert(Object object) {
        session.insert(object);
        return 0;
    }

    @Override
    public int update(Object object) {
        FactHandle handle = session.getFactHandle(object);
        session.update(handle,object);
        return 1;
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
