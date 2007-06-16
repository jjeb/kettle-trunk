package be.ibridge.kettle.trans;

import be.ibridge.kettle.trans.step.StepMeta;
import be.ibridge.kettle.trans.step.StepMetaInterface;
import be.ibridge.kettle.trans.step.dummytrans.DummyTransMeta;

public class TransPreviewFactory
{
    public static final TransMeta generatePreviewTransformation(StepMetaInterface oneMeta, String oneStepname)
    {
        StepLoader stepLoader = StepLoader.getInstance();

        TransMeta previewMeta = new TransMeta();
        
        // At it to the first step.
        StepMeta one = new StepMeta(stepLoader.getStepPluginID(oneMeta), oneStepname, oneMeta);
        one.setLocation(50,50);
        one.setDraw(true);
        previewMeta.addStep(one);
        
        DummyTransMeta twoMeta = new DummyTransMeta();
        StepMeta two = new StepMeta(stepLoader.getStepPluginID(twoMeta), "dummy", twoMeta); //$NON-NLS-1$
        two.setLocation(250,50);
        two.setDraw(true);
        previewMeta.addStep(two);
        
        TransHopMeta hop = new TransHopMeta(one, two);
        previewMeta.addTransHop(hop);

        return previewMeta;
    }
}
