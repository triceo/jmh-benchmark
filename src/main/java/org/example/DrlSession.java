package org.example;

import org.drools.modelcompiler.ExecutableModelProject;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.builder.conf.PropertySpecificOption;
import org.kie.internal.utils.KieHelper;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.buildin.simple.SimpleScoreHolder;
import org.optaplanner.core.impl.score.director.drools.DroolsScoreDirectorFactory;

final class DrlSession implements Session {

    private static final String DRL = "import " + SimpleScoreHolder.class.getCanonicalName() + ";\n" +
            "import " + MyFact.class.getCanonicalName() + ";\n" +
            "global SimpleScoreHolder scoreHolder;\n" +
            "rule \"Join\"\n" +
            "    when\n" +
            "        $fact : MyFact()\n" +
            "        MyFact(id > $fact.id)\n" +
            "    then\n" +
            "        // don't do anything\n" +
            "end\n";
    private static final DroolsScoreDirectorFactory<MySolution, ?> SDF =
            new DroolsScoreDirectorFactory<>(MyBenchmark.SOLUTION_DESCRIPTOR, buildKieBase());
    private final KieSession session;

    public DrlSession() {
        session = SDF.newKieSession();
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
    public int update(Object object) {
        FactHandle handle = session.getFactHandle(object);
        session.update(handle,object);
        return 1;
    }

    @Override
    public SimpleScore calculateScore() {
        int fireCount = session.fireAllRules();
        return SimpleScore.of(fireCount);
    }

    @Override
    public void close() {
        session.dispose();
    }
}
