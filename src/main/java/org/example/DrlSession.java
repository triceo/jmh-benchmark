package org.example;

import org.drools.modelcompiler.ExecutableModelProject;
import org.example.domain.MyFact;
import org.example.domain.MySolution;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.conf.PropertySpecificOption;
import org.kie.internal.event.rule.RuleEventManager;
import org.kie.internal.utils.KieHelper;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.buildin.simple.SimpleScoreHolder;
import org.optaplanner.core.impl.score.buildin.simple.SimpleScoreHolderImpl;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;
import org.optaplanner.core.impl.score.director.drools.OptaPlannerRuleEventListener;

final class DrlSession implements Session {

    private final KieSession session;
    private final SimpleScoreHolderImpl scoreHolder;

    private static String getDrl(int joinCount) {
        String drl = "import " + SimpleScoreHolder.class.getCanonicalName() + ";\n" +
                "import " + MyFact.class.getCanonicalName() + ";\n" +
                "global SimpleScoreHolder scoreHolder;\n" +
                "rule \"Join\"\n" +
                "    when\n" +
                "        $fact1 : MyFact()\n" +
                "        $fact2 : MyFact(joinId == $fact1.joinId)\n";
        if (joinCount > 1) {
            drl +=
                    "        $fact3 : MyFact(joinId == $fact2.joinId)\n";
        }
        if (joinCount > 2) {
            drl +=
                    "        $fact4 : MyFact(joinId == $fact3.joinId)\n";
        }
        return drl +
                "    then\n" +
                "        scoreHolder.addConstraintMatch(kcontext, 1);\n" +
                "end\n";
    }

    public DrlSession(int joinCount) {
        DroolsScoreDirectorFactory<MySolution, ?> sdf =
                new DroolsScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, buildKieBase(getDrl(joinCount)));
        scoreHolder = new SimpleScoreHolderImpl(false);
        session = sdf.newKieSession();
        ((RuleEventManager) session).addEventListener(new OptaPlannerRuleEventListener());
        session.setGlobal("scoreHolder", scoreHolder);
    }

    private static KieBase buildKieBase(String drl) {
        return new KieHelper(PropertySpecificOption.DISABLED)
                .addContent(drl, ResourceType.DRL)
                .build(ExecutableModelProject.class);
    }

    @Override
    public int insert(Object object) {
        session.insert(object);
        return MyBenchmark.RANDOM.nextInt();
    }

    @Override
    public int update(Object object) {
        FactHandle handle = session.getFactHandle(object);
        session.update(handle, object);
        return MyBenchmark.RANDOM.nextInt();
    }

    @Override
    public SimpleScore calculateScore() {
        int fireCount = session.fireAllRules();
        return scoreHolder.extractScore(fireCount);
    }

    @Override
    public void close() {
        session.dispose();
    }
}
