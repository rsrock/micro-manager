///////////////////////////////////////////////////////////////////////////////
//FILE:          AutofocusUtils.java
//PROJECT:       Micro-Manager 
//SUBSYSTEM:     ASIdiSPIM plugin
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman, Jon Daniels
//
// COPYRIGHT:    University of California, San Francisco, & ASI, 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.asidispim.Utils;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author nico
 */
public class FitUtils {
   public static enum FunctionType {Pol2, Pol3, Gaussian};
   
   /**
    * Utility to facilitate fitting data plotted in JFreeChart
    * Provide data in JFReeChart format (XYSeries), and retrieve univariate
    * function parameters that best fit (using least squares) the data. All data
    * points will be weighted equally.
    * 
    * TODO: investigate whether weighting (possibly automatic weighting) can
    * improve accuracy
    * 
    * @param data xy series in JFReeChart format
    * @param type one of the FitUtils.FunctionType predefined functions
    * @param guess initial guess for the fit.  The number and meaning of these
             parameters depends on the FunctionType.  Implemented:
             Gaussian: 0: Normalization, 1: Mean 2: Sigma

    * @return array with parameters, whose meaning depends on the FunctionType.
    *          Use the function getXYSeries to retrieve the XYDataset predicted 
    *          by this fit
    */
   public static double[] fit(XYSeries data, FunctionType type, double[] guess) {
      
      // create the commons math data object from the JFreeChart data object
      final WeightedObservedPoints obs = new WeightedObservedPoints();
      for (int i = 0; i < data.getItemCount(); i++) {
         obs.add(1.0, data.getX(i).doubleValue(), data.getY(i).doubleValue());
      }
      
      double[] result = null;
      switch (type) {
         case Pol2:
            final PolynomialCurveFitter fitter2 = PolynomialCurveFitter.create(2);
            result = fitter2.fit(obs.toList());
            break;
         case Pol3:
            final PolynomialCurveFitter fitter3 = PolynomialCurveFitter.create(3);
            result = fitter3.fit(obs.toList());
            break;
         case Gaussian:
            final GaussianCurveFitter gf = GaussianCurveFitter.create();
            if (guess != null) {
               gf.withStartPoint(guess);
            }
            result = gf.fit(obs.toList());
      }
      
      return result;
   }
   
   /**
    * Given a JFreeChart dataset and a commons math function, return a JFreeChart
    * dataset in which the original x values are now accompanied by the y values
    * predicted by the function
    * 
    * @param data input JFreeChart data set
    * @param type one of the FitUtils.FunctionType predefined functions
    * @param parms parameters describing the function.  These need to match the
    *             selected function or an IllegalArgumentEception will be thrown
    * 
    * @return JFreeChart dataset with original x values and fitted y values.
    */
   public static XYSeries getFittedSeries(XYSeries data, FunctionType type, 
           double[] parms) {
      XYSeries result = new XYSeries(data.getItemCount());
      switch (type) {
         case Pol2:
            if (parms.length != 2)
               throw new IllegalArgumentException("Needs a double[] of size 2");
         case Pol3:
            if (parms.length != 3)
               throw new IllegalArgumentException("Needs a double[] of size 3");
            PolynomialFunction polFunction = new PolynomialFunction(parms);
            for (int i = 0; i < data.getItemCount(); i++) {
               double x = data.getX(i).doubleValue();
               double y = polFunction.value(x);
               result.add(x, y);
            }
            break;
         case Gaussian:
            Gaussian.Parametric gf = new Gaussian.Parametric();
            for (int i = 0; i < data.getItemCount(); i++) {
               double x = data.getX(i).doubleValue();
               double y = gf.value(x, parms);
               result.add(x, y);
            }
            break;
      }
      
      return result;
   }
   
   /**
    * Finds the x value corresponding to the maximum function value within the 
    * range of the provided data set
    * 
    * @param type one of the FitUtils.FunctionType predefined functions
    * @param parms parameters describing the function.  These need to match the
    *             selected function or an IllegalArgumentEception will be thrown
    * @param data JFreeChart series, used to bracket the range in which the 
    *             maximum will be found
    * 
    * @return x value corresponding to the maximum function value
    */
   public double getMaxX(FunctionType type, double[] parms, XYSeries data) {
      double xMax = 0.0;
      double minRange = data.getMinX();
      double maxRange = data.getMaxX();
      switch (type) {
         case Pol2:
            if (parms.length != 2)
               throw new IllegalArgumentException("Needs a double[] of size 2");
         case Pol3:
            if (parms.length != 3)
               throw new IllegalArgumentException("Needs a double[] of size 3");  
            PolynomialFunction derivativePolFunction = 
                    (new PolynomialFunction(parms)).polynomialDerivative();

            final double relativeAccuracy = 1.0e-12;
            final double absoluteAccuracy = 1.0e-8;
            final int    maxOrder         = 5;
            UnivariateSolver solver = 
                    new BracketingNthOrderBrentSolver(relativeAccuracy, 
                            absoluteAccuracy, maxOrder);
            xMax = solver.solve(100, derivativePolFunction, minRange, maxRange);
            break;
         case Gaussian:
            // for a Gaussian we can take the mean and be sure it is the maximum
            xMax = parms[1];
      }
              
      return xMax;
   }
   
}