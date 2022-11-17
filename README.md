## Social Adaptive Ensembles

Modifications in SAE2 and SFNClassifier algorithms to run in newer versions of [MOA](https://github.com/Waikato/moa)

For more details about the algorithms: 

- [SFNClassifier](https://dl.acm.org/doi/10.1145/2554850.2554855): a scale-free social network method to handle concept drift, from Barddal et.al

- [SAE2](https://dl.acm.org/doi/10.1145/2554850.2554905): advances on the social adaptive ensemble classifier for data streams, from Gomes et. al

## Ｈｏｗ ｔｏ ｒｕｎ <img src = "https://media1.giphy.com/media/JZ40cnfnN11KycrvMF/giphy.gif?cid=ecf05e47a0n3gi1bfqntqmob8g9aid1oyj2wr3ds3mg700bl&rid=giphy.gif" width = 23px>

-   Insert the sae folder into MOA classifiers folder

-   Then you will be able to run the following command in prompt:

```yaml
java -cp moa.jar -javaagent:.\sizeofag-1.0.4.jar moa.DoTask \"EvaluatePrequential -l (sae.meta.SFNClassifier) -e (BasicClassificationPerformanceEvaluator -o ) {dataset}
```