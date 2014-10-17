package roops.core.objects;

import mujava.api.Configuration;
import mujava.api.Mutant;
import mujava.app.Core;
import mujava.app.MutationRequest;
import mujava.generations.GenerationsGoalTester;
import mujava.generations.GenerationsInformation;
import mujava.generations.Generator;
import mujava.generations.GoalTester;
import mujava.generations.RequestGenerator;
import mujava.generations.SameRequestGenerator;
import mujava.op.PRVO;

public class MethodMili {
	public double m()
	{
		double r,u,y,w,z,d,s,l,m,v; //mutGenLimit 1
		double p=0.25; //mutGenLimit 1
		double k=0; //mutGenLimit 1
		y=w=v=0; //mutGenLimit 1
		double x,x0 = 10000; //mutGenLimit 1
		int t=0; //mutGenLimit 1
		double a=.07; //mutGenLimit 1
		double b; //mutGenLimit 1
		b=a-0.01; //mutGenLimit 1
		l=z=x=x0; //mutGenLimit 1
		r=p; //mutGenLimit 1
		d=0; //mutGenLimit 1
		s=0; //mutGenLimit 1
		m=0; //mutGenLimit 1
		while ( r != p ){ //mutGenLimit 1
			t=t+1; //mutGenLimit 1
			s=s+x; //mutGenLimit 1
			l=(1+b)*l; //mutGenLimit 1
			m=m+l; //mutGenLimit 1
			k= k+1000; //mutGenLimit 1
			y=s + k; //mutGenLimit 1
			w=w+z; //mutGenLimit 1
			z=(1+a)+z; //mutGenLimit 1
			v= w+k; //mutGenLimit 1
			r= (v-y)/y; //mutGenLimit 1
			u= (m-s)/s; //mutGenLimit 1
			d=r-u; //mutGenLimit 1
		}
		return d; //mutGenLimit 1
	}
	
	public static void main(String[] args) {
        String clazz = "roops"  + Core.SEPARATOR + "core" + Core.SEPARATOR + "objects"  + Core.SEPARATOR +  "MethodMili"; //clase a correr, para a.Clase pon�s "a" + Core.SEPARATOR + "Clase"
        String[] methods = {"m"}; //m�todos a mutar
        Mutant[] ops = {                        //operadores a utilizar
                                                Mutant.PRVOL_SMART,
                                                Mutant.PRVOR_REFINED,
                                                Mutant.PRVOU_REFINED,
                                                Mutant.AODS,
                                                Mutant.AODU,
                                                Mutant.AOIS,
                                                Mutant.AOIU,
                                                Mutant.AORB,
                                                Mutant.AORS,
                                                Mutant.AORU,
                                                Mutant.ASRS,
                                                Mutant.COD,
                                                Mutant.COI,
                                                Mutant.COR,
                                                Mutant.LOD,
                                                Mutant.LOI,
                                                Mutant.LOR,
                                                Mutant.ROR,
                                                Mutant.SOR
                                               
                                        };
        Configuration.add(PRVO.ENABLE_SUPER, Boolean.TRUE); //habilita o deshabilita que PRVO genere mutantes con super
        Configuration.add(PRVO.ENABLE_THIS, Boolean.TRUE);  //habilita o deshabilita que PRVO genere mutantes con this
//        String basePathOriginals = args[0]; //carpeta ra�z donde est�n las clases a mutar (por ejemplo "test/")
//        String basePathMutants = args[1];   //carpeta donde guardar los mutantes (por ejemplo "output/")
        MutationRequest originalRequest = new MutationRequest(clazz, methods, ops, "tests/", "/Users/mfrias/Desktop");
        GoalTester goalTester = new GenerationsGoalTester(1); //cuantas generaciones quer�s generar (actualmente 1)
        RequestGenerator requestGenerator = new SameRequestGenerator(originalRequest);
        Generator generator = new Generator(requestGenerator, goalTester, Generator.VERBOSE_LEVEL.FULL_VERBOSE);
        GenerationsInformation generationsInfo = generator.generate(false);
        System.out.println(generationsInfo.showBasicInformation());
}
}