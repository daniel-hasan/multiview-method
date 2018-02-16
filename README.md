# Multiview Machine Learning Method

This is the machine learning method implemented for the [article](http://onlinelibrary.wiley.com/doi/10.1002/asi.23650/full): 

> Dalip, Daniel Hasan, Gonçalves, Marcos A., Cristo, Marco, & Calado, Pavel (2017). **A general multiview framework for assessing the quality of collaboratively created content on web 2.0**. Journal of the Association for Information Science and Technology, 68(2), 286-308.


This software is compatible with Linux only.

## Usage



First, prepare a training and testing dataset in libsvm the format: 

```
<class> <id_feature_1>:<val_feature_1> <id_feature_2>:<val_feature_2> ... <id_feature_n>:<val_feature_n>
```

where `<class>` is the target class `<id_feature_i>` is the ith feature id (starting with 1) and `<val_feature_i>` is the ith feature value. After that, configure the multiview method setting the config variables (you can change the default parameters in the [configExample.cnf](configExample.cnf) or [configExample_l2r.cnf](configExample_l2r.cnf)). Also in this file you will assign the view for each feature. For more information, see `.cnf` file comments. To run this program, use: 

```
java -jar multiview.jar <train-file> <test-file> <config-file>
```

Example:

```
java -jar multiview.jar toyExample/train_svm.txt toyExample/test_svm.txt configExample.cnf
```

The source code is available at the `MultiviewMethod` folder. Note that you can use different methods by changing the XML [MultiviewMethod/learning_methods.xml](MultiviewMethod/learning_methods.xml) and creating their scripts. Regarding Learning to Rank method, we used the SVM-RANK library available at: [https://www.cs.cornell.edu/peopl](https://www.cs.cornell.edu/peopl). Use the same format as SVM-RANK in case of L2R problems.



## Quality Assessment Method Datasets and Results

The dataset and results for assessing the quality of content regarding the [Question and Answering Forums](http://www.lbd.dcc.ufmg.br/lbd/collections/ranking-q-a-forums) and [Wikis](http://www.lbd.dcc.ufmg.br/lbd/collections/wiki-quality) datasets are avaliable for download in their respective links. 


