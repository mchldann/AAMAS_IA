# Intention-Aware Multiagent Scheduling

This repo contains code for the paper [Intention-Aware Multiagent Scheduling](https://www.ifaamas.org/Proceedings/aamas2020/pdfs/p285.pdf) (AAMAS'20) by Michael Dann, Yuan Yao, John Thangarajah and Brian Logan.

## Abstract

The Belief Desire Intention (BDI) model of agency is a popular and mature paradigm for designing and implementing multiagent systems. There are several agent implementation platforms that follow the BDI model. In BDI systems, the agents typically have to pursue multiple goals, and often concurrently. The way in which the agents commit to achieving their goals forms their intentions. There has been much work on scheduling the intentions of agents. However, most of this work has focused on scheduling the intentions of a single agent with no awareness and consideration of other agents that may be operating in the same environment. They schedule the intentions of the single-agent in order to maximise the total number of goals achieved. In this work, we investigate techniques for scheduling the intentions of an agent in a multiagent setting, where an agent is aware (or partially aware) of the intentions of other agents in the environment. We use a Monte Carlo Tree Search (MCTS) based approach and show that our intention-aware scheduler generates better outcomes in cooperative, neutral (selfish) and adversarial settings than the state-of-the-art schedulers that do not consider other agents' intentions.

## Disclaimer

The code contained in this repo is very much "research code". It has been hacked together from multiple sources, resulting in numerous public variables, variables whose names don't match the paper, etc. We'd like to tidy it up some day, but no promises!

## Running

Run the Main class from within the default package without any command line arguments. (The configure the experiment parameters, edit Main.java.)
