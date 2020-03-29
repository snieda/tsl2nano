package de.tsl2.nano.aspect;

import java.lang.annotation.Annotation;

/**
 * used by AspectCover to provide a covered method call the aspect arguements
 */
public class CoverArgs {
    /** calling object */
    Object this_;
    /** parent object */
    Object target;
    /** method name */
    String name;
    /** method arguments */
    Object[] args;
    /** -1:before, 0:body, 1:after */
    int inspectionPoint;

    public CoverArgs(String name, Object this_, Object target, Class<? extends Annotation> inspectionPoint, Object[] args) {
        this.this_ = this_;
        this.target = target;
        this.name = name;
        this.args = args;
        this.inspectionPoint = inspectionPoint.equals(CoverBefore.class) ? -1 : inspectionPoint.equals(CoverBody.class) ? 0 : 1;
    }

}