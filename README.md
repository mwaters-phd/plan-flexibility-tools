# Plan Optimisation Tools

This repository countains two programs, MRR and MKTR, that relax plans into more flexible, generalised forms.
MRR finds a *minimum reinstantiated reorder* of the input plan, that is, it searches for the set of variable bindings that allow for the greatest reduction in its ordering constraints.
MKTR relaxes a plan into a minimal *partial plan*, which specifies which operators must be executed without fully specifying their order or their variable bindings.

For a complete description of MRR please see the IJCAI 2020 paper by Waters et al., [*Optimising Partial-Order Plans Via Action Reinstantiation*](https://www.ijcai.org/Proceedings/2020/573), and for MKTR, please see the ICAPS 2018 paper by Waters et al., [*Plan Relaxation via Action Debinding and Deordering*](https://www.aaai.org/ocs/index.php/ICAPS/ICAPS18/paper/viewPaper/17765)

## Compilation

Download the following `.jar` files to the `lib` directory:

* [`args4j-2.33.jar`](https://github.com/kohsuke/args4j)
* [`libtw.jar`](http://www.treewidth.com/treewidth/)
* [`pddl4j-3.5.0.jar`](https://github.com/pellierd/pddl4j)

The following programs must be installed:

* Java SDK 1.8+
* [ant](http://ant.apache.org)
* [Python 3](https://www.python.org/)

From the root directory run `ant build`. The required Java code will be compiled into `lib/pplib-0.1.2.jar`.

## Dependencies

MRR requires the following programs:

* [Loandra](https://github.com/jezberg/loandra)
* [MaxPre](https://github.com/Laakeri/maxpre)
* [Nauty](https://pallini.di.uniroma1.it/)

The programs `dreadnaut`, `loandra` and `maxpre` must be in the `PATH`.

MKTR requires the following programs:

* [Minizinc](http://www.minizinc.org)
* [Gecode flatzinc interpreter](http://www.gecode.org/flatzinc.html)
* [treewidth-exact](https://github.com/TCS-Meiji/treewidth-exact)
* [nauty](https://pallini.di.uniroma1.it/)

The programs `dreadnaut`, `tw-exact`, `mzn2fzn` and `fzn-gecode` must be in the `PATH`.

## Supported PDDL fragments

MRR and MKTR support all features of basic `STRIPS`, and some aspects of `ADL`, namely `equality`, `typing` and `negative preconditions`.

## Plan format

Plan files with a `.m` extension are assumed to be [Madagascar](https://research.ics.aalto.fi/software/sat/madagascar/) parallel plans (e.g., `example/p01.pddl.m`), otherwise plans are assumed to be standard IPC format plans (e.g., `example/p01.pddl.bfws`).


# MRR: Minimum Reinsantiated Reorder

MRR is an implementation of a MaxSAT-based technique for finding *minimum deorderings and reorderings* of a partial order plan. It can be configured to either find a *minimum deorder* or *minimum reorder* as per [Muise et al.](https://www.jair.org/index.php/jair/article/view/11024), or a *minimum reinstantiated deorder* or *minimum reinstantiated reorder* as per [Waters et al.](https://www.ijcai.org/Proceedings/2020/573).
Some configurations can be extended with symmetry breaking constraints that reduce the MaxSAT solving time.

## Running MRR

Run MRR with the following command:

```
mrr.py [-h] --dfile DOMAIN --ifile PROBLEM --pfile PLAN --encoder ENCODER [--verbose]
			  	 
```

Required arguments:

* `--dfile DOMAIN`: The location of the PDDL domain file.
* `--ifile PROBLEM`: The location of the problem instance PDDL file.
* `--pfile PLAN`: The location of the plan which solves `PROBLEM`.
* `--encoder ENCODER`: The encoder used to convert the input plan into a MaxSAT instance. Options are: 
	* `MD`: Finds a minimum deorder as per Muise et al.
	* `MR`: Finds a minimum reorder as per Muise et al.
	* `MR_OPSB`: Finds a minimum reorder, also encodes operator symmetry breaking constraints.
	* `MRD`: Finds a minimum reinstantiated deorder.
	* `MRR`: Finds a minimum reinstantiated reorder.
	* `MRR_OPSB`: Finds a minimum reinstantiated reorder, also encodes operator symmetry breaking constraints.
    * `MRR_CSSB`: Finds a minimum reinstantiated reorder, also encodes causal structure symmetry breaking constraints.
	
Options:

* `--verbose`: Verbose output.
* `--time TIME`: Time limit (in minutes). Default is 30.


## Example

The `example` directory contains a small planning instance from the IPC `rovers` domain, and three different plans. Run MRR over the example with the following commmand:

```
./mktr.sh --dfile example/domain.pddl --ifile example/p01.pddl
		  --pfile example/p01.pddl.m --alg MRR --verbose
			  	 
```

# MKTR: Minimal k-Treewidth Relaxation

MKTR is an application for finding *re-instantiations* of a plan, i.e., alternative plans which achieve the same goal and use the same actions, but differ in the order of those actions and how the variables in those actions have been bound.

A set of re-instantiations can be compactly represented as a *constraint formula*, i.e., a formula expressed in a fragment of first-order logic, where each model represents an alternative to the original plan.
While computing a model of a constraint formula is an NP-complete problem, if the formula's *treewidth* is bounded by `k`, a model can be found in time `O(n^k)`.

MKTR first generates a constraint formula with just one model, the original plan.
It then iteratively and greedily relaxes this formula, while keeping its treewidth below an input value.
When the formula cannot be relaxed any further without its treewidth exceeding the input value, the final constraint formula is returned.

In this program, constraint formulae are implemented as constraint satisfaction problems (CSPs).
The output to the MKTR is a CSP of bounded treewidth, each solution to which represents a different re-instantiation of the input plan.

## Running MKTR

Run MKTR with the following command:


```
mktr.py [-h] --dfile DOMAIN --ifile PROBLEM --pfile PLAN 
             --tw TW --pol POL 
             [--time TIME] [--validate] [--verbose] [--count] 	 
```

Required arguments:

* `--dfile DOMAIN`: The location of the PDDL domain file.
* `--ifile PROBLEM`: The location of the problem instance PDDL file.
* `--pfile PLAN`: The location of the plan which solves `PROBLEM`.
* `--tw TW`: The maximum allowed treewidth.
* `--pol POL`: The relaxation policy. Options are: 
	* `MinimiseThreats`: Add the causal link with the fewest threats.
	* `MinimiseThreatsMultiLex`: As `MinimiseThreats` but with additional causal structure symmetry breaking.
    * `RelaxProducers`: Relax the bindings of producers with many consumers.
	* `RelaxProducersMultiLex`: As `RelaxProducers` but with additional causal structure symmetry breaking.
  
Options:

* `--verbose`: Verbose output. Reinstantiations will be counted at each iteration of MKTR.
* `--count`: Count the number of reinstantiations represented by the final CSP.
* `--validate`: Validate the reinstantiations. Requires either `--verbose` or `--count`.
* `--time TIME`: Time limit (in minutes). Default is 30.

## Example

The `example` directory contains a small planning instance from the IPC `rovers` domain, and three different plans. The following command will run MKTR with a treewidth of 2 and the `MinimiseThreats` policy over the example, and count the number of resulting re-instantiations:

```
./mktr.py --dfile ./example/domain.pddl --ifile ./example/p01.pddl 
          --pfile ./example/p01.pddl.m --tw 2 --pol MinimiseThreats --count
			  	 
```

# Contact

Max Waters (max.waters@rmit.edu.au).
 
# License

This project is using the GPLv3 for open source licensing for information and the license visit GNU website (https://www.gnu.org/licenses/gpl-3.0.en.html).

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.
