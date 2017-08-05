
uint rand1(uint seed1, uint seed2){
    uint seed = seed1;
    uint t = seed ^ (seed << 11);
    return seed2 ^ (seed2 >> 19) ^ (t ^ (t >> 8));
}

uint rand2(ulong seed){
    ulong t = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    return t >> 16;
}

inline int get2DFrom1D(const global int* array1D, int i, int j, int arraySize){
    return array1D[j*arraySize+i];
}

inline int getLocalIdInSerializedResult(int result){
    return (result & 0xFF000000)>>24;
}
inline int getCostFunctionValueInSerializedResult(int result){
    return result & 0x00FFFFFF;
}
inline int serializeIdAndCostFunctionValue(int id, int cost){
    return (id<<24)|cost;
}

//copy from durstenfldAlgorithm
void createFoodSources(local int * foodSources, ulong* lastRand, int problemSize){
    int globalId = get_global_id(0);
    int localId = get_local_id(0);

    int i, j, tmp;

    for(i = 0; i<problemSize; i++){
        foodSources[localId * problemSize + i] = i;
    }

    for(i = problemSize - 1; i>0;i--){
        *lastRand = rand2(*lastRand);
        j =*lastRand % (i+1);
        tmp = foodSources[localId * problemSize + i] ;
        foodSources[localId * problemSize + i] = foodSources[localId * problemSize + j];
        foodSources[localId * problemSize + j] = tmp;
    }
}

void evaluateCostFunction(local int * foodSources, local int * costFunctionValues, const global int* weights, const global int* distances, const int problemSize){
    int localId = get_local_id(0);
    costFunctionValues[localId] = 0;
    for (int i = 0; i < problemSize; i++) {
        for (int j = i+1; j < problemSize; j++) {
            costFunctionValues[localId] += get2DFrom1D(weights, foodSources[localId*problemSize +i], foodSources[localId*problemSize +j], problemSize) * get2DFrom1D(distances,i, j, problemSize);
        }
    }
    //printf((__constant char *)"%d cost function value = %d \n", localId, costFunctionValues[localId]);

}
void employeeBees(local int * foodSources, local int * costFunctionValues , local int * tries ,const global int* weights, const global int* distances, int problemSize, ulong* lastRand){
    int localId = get_local_id(0);
    int i,j;

    *lastRand = rand2(*lastRand);
    i = *lastRand  % (problemSize);
    *lastRand = rand2(*lastRand);
    j =*lastRand % (problemSize);

    //UWAGA mozna zrobic reuzycie zmiennej lastRand jako oldCost

    int newCost = costFunctionValues[localId];
    int iVal = foodSources[localId * problemSize + i];
    int jVal = foodSources[localId * problemSize + j];
    for(int a = 0;a<problemSize;a++){
        if(a!=i && a!=j){
            newCost+=((distances[problemSize*i+a] - distances[problemSize*j+a]) *  (weights[jVal * problemSize + foodSources[localId * problemSize + a]] - weights[iVal * problemSize + foodSources[localId * problemSize + a]]));
        }
    }
    //printf((__constant char *)"%d  last cost: %d , new cost: %d, i: %d, j: %d\n", localId, costFunctionValues[localId], newCost, i,j);

    if(newCost < costFunctionValues[localId] ){
        costFunctionValues[localId] = newCost;
        foodSources[localId * problemSize + i] = jVal;
        foodSources[localId * problemSize + j] = iVal;
        tries[localId]=0;
    }else{
        tries[localId]++;
    }
}

void prefixSum(local int* costFunctionValues, local int* tmpTab) {
	int tid = get_local_id(0);
    uint length = get_local_size(0);
	int offset = 1;

	/* Cache the computational window in shared memory */
	tmpTab[ tid] = costFunctionValues[tid];

	/* build the sum in place up the tree */
	for (int d = length >> 1; d > 0; d >>= 1){
		barrier (CLK_LOCAL_MEM_FENCE);

		if (tid < d){
			int ai = offset * (2 * tid + 1) - 1;
			int bi = offset * (2 * tid + 2) - 1;

			tmpTab[bi] += tmpTab[ai];
		}
		offset *= 2;
	}

	/* scan back down the tree */

	/* clear the last element */
	if (tid == 0){
		tmpTab[length - 1] = 0;
	}

	/* traverse down the tree building the scan in the place */
	for (int d = 1; d < length; d *= 2)	{
		offset >>= 1;
		barrier (CLK_LOCAL_MEM_FENCE);

		if (tid < d){
			int ai = offset * (2 * tid + 1) - 1;
			int bi = offset * (2 * tid + 2) - 1;

			int t = tmpTab[ai];
			tmpTab[ai] = tmpTab[bi];
			tmpTab[bi] += t;
		}
	}

	barrier (CLK_LOCAL_MEM_FENCE);
}

void onlookerBee(local int * foodSources, local int * costFunctionValues, local int * tries,const global int* weights, const global int* distances, int chosenFoodSource,  int problemSize, ulong * lastRand, local int * tmpTab){
    int localId = get_local_id(0);
    int bestCostFunction = costFunctionValues[chosenFoodSource];
    int i,j;

    *lastRand = rand2(*lastRand);
    i = *lastRand  % (problemSize);
    *lastRand = rand2(*lastRand);
    j =*lastRand % (problemSize);

    int newCost = bestCostFunction;
    int iVal = foodSources[chosenFoodSource * problemSize + i];
    int jVal = foodSources[chosenFoodSource * problemSize + j];
    for(int a = 0;a<problemSize;a++){
        if(a!=i && a!=j){
            newCost+=((distances[problemSize*i+a] - distances[problemSize*j+a]) *  (weights[jVal * problemSize + foodSources[chosenFoodSource * problemSize + a]] - weights[iVal * problemSize + foodSources[chosenFoodSource * problemSize + a]]));
        }
    }


    //celowo 0 jako id, gdyż w tym kontekście nie ma on sensu
    tmpTab[localId] = serializeIdAndCostFunctionValue(0, costFunctionValues[localId]);
    int mySerialized = serializeIdAndCostFunctionValue(localId, newCost);
    barrier (CLK_LOCAL_MEM_FENCE);
    while(newCost < getCostFunctionValueInSerializedResult(tmpTab[chosenFoodSource]) ){
        //barrier (CLK_LOCAL_MEM_FENCE);
        tmpTab[chosenFoodSource] = mySerialized;
    }
    barrier (CLK_LOCAL_MEM_FENCE);

    //poniższy if/else wykonywany jest dla indeksów lokalnych przez co każdy indeks lokalny jest obsługiwany przez dokładnie jeden
    //wątek a dzięki temu wyzerowanie lub ewentualna inkrementacja wystąpią tylko raz dla każdego elementu
    //Aby zliczyć wszystkie próby, mozna ten if wykonać indeksów chosenFoodSource, które mogą się powtarzać. Wtedy należy jednak użyć funkcji atomic_add w przypadku inkrementacji
    if( getCostFunctionValueInSerializedResult(tmpTab[localId]) <costFunctionValues[localId]){
        tries[localId]=0;
    }else{
        tries[localId]++;
    }

    //Następny if jest już wykonywany w kontekście chosenFoodSource ponieważ będą używane zmienne z tego wątku: i,iVal,j,jVal
    if( getCostFunctionValueInSerializedResult(tmpTab[chosenFoodSource])< costFunctionValues[chosenFoodSource]
        && getLocalIdInSerializedResult(tmpTab[chosenFoodSource]) == localId){
        foodSources[chosenFoodSource * problemSize + i] = jVal;
        foodSources[chosenFoodSource * problemSize + j] = iVal;
        costFunctionValues[chosenFoodSource]=newCost; //inaczej getCostFunctionValueInSerializedResult(tmpTab[chosenFoodSource]
    }
}

int lowerBound( local int * tab, int value){
    return 0;
}

void originalSelection(local int * foodSources, local int * costFunctionValues , local int * tries ,const global int* weights, const global int* distances, int problemSize, ulong * lastRand, int onlookersNumberInGroup, local int* tmpTab){
    int localId = get_local_id(0);

    prefixSum(costFunctionValues, tmpTab);
    if(localId == 0){
        for(int i =0;i< get_local_size(0);i++){
            printf((__constant char *)"%d v=%d ps=%d\n", i,costFunctionValues[i],tmpTab[i]);
        }
    }
    *lastRand = rand2(*lastRand);

    int randValue = *lastRand  % (tmpTab[get_local_size(0)-1]);

    int chosenFoodSource = lowerBound(tmpTab,randValue );

    onlookerBee(foodSources, costFunctionValues, tries,weights, distances,   chosenFoodSource, problemSize, lastRand, tmpTab);
}

void eliteSelection(local int * foodSources, local int * costFunctionValues , local int * tries ,const global int* weights, const global int* distances, int problemSize, ulong * lastRand, int onlookersNumberInGroup, local int * tmpTab ){
    int localId = get_local_id(0);
    if (localId >= onlookersNumberInGroup)  {
        return;
    }

    int i,j, chosenFoodSource, bestCostFunction;
    int foodSourcesNumberInGroup = get_local_size(0);
    int foodSourcesPerOnlooker = foodSourcesNumberInGroup / onlookersNumberInGroup;
    int leftItems = foodSourcesNumberInGroup - foodSourcesPerOnlooker * onlookersNumberInGroup;

    if(localId >=onlookersNumberInGroup){
        return;
    }
    else if(localId < leftItems){
        i = localId * (foodSourcesPerOnlooker + 1);
        j = i + foodSourcesPerOnlooker + 1;
    }else{
        i = localId * foodSourcesPerOnlooker + leftItems;
        j = i + foodSourcesPerOnlooker;
    }

    chosenFoodSource = i;
    for(; i<j; i++){
        if(costFunctionValues[i] < bestCostFunction){
            chosenFoodSource = i;
            bestCostFunction = costFunctionValues[i];
        }
    }
    onlookerBee(foodSources, costFunctionValues, tries,weights, distances,   chosenFoodSource, problemSize, lastRand, tmpTab);
}

void scoutBees(local int * foodSources, local int * tries , int problemSize, ulong * lastRand, int threshold){
    int localId = get_local_id(0);
    int i, j, tmp;

    if(tries[localId]>= threshold){
        for(i = 0; i<problemSize; i++){
            foodSources[localId * problemSize + i] = i;
        }

        for(i = problemSize - 1; i>0;i--){
            *lastRand = rand2(*lastRand);
            j =*lastRand % (i+1);
            tmp = foodSources[localId * problemSize + i] ;
            foodSources[localId * problemSize + i] = foodSources[localId * problemSize + j];
            foodSources[localId * problemSize + j] = tmp;
        }
        tries[localId] =0;
    }
}

void updateLocalBestInLoop( local int* costFunctionValues, local int* bestIndicator){
    int localId = get_local_id(0);
    int serializedLocalCostFunctionValue = serializeIdAndCostFunctionValue(localId, costFunctionValues[localId]);
    while(costFunctionValues[localId] < (*bestIndicator & 0x00FFFFFF)){
        *bestIndicator = serializedLocalCostFunctionValue;
    }
}

void findBestReduction(local int* costFunctionValues, local int* tmpTab, local int* bestIndicator) {
    int localId = get_local_id(0);

    // Load data into local memory
    tmpTab[localId] = serializeIdAndCostFunctionValue(localId, costFunctionValues[localId]);

    barrier(CLK_LOCAL_MEM_FENCE);
    for(int offset = get_local_size(0) / 2;offset >0;offset >>= 1) {
        if (localId < offset) {
            if( getCostFunctionValueInSerializedResult( tmpTab[localId]) >  getCostFunctionValueInSerializedResult( tmpTab[localId + offset])){
                tmpTab[localId] = tmpTab[localId + offset];
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    if (localId == 0) {
        *bestIndicator = tmpTab[0];
    }
}

kernel void wholeAlgorithmKernel(const global int* weights, const global int* distances, const int onlookersNumberInGroup, const int problemSize, const int maxIterations, const int maxTries,ulong hostSeed, global int * solutionBuffer, local int * foodSources, local int * costFunctionValues, local int * tries, local int * tmpTab,short useReductionToFindBest, short selectionMethod ) {
    // get index into global data array
    int globalId = get_global_id(0);
    int localId = get_local_id(0);
    int iteration;
    ulong lastRand = hostSeed + globalId;

    local int bestIndicator;
    bestIndicator = 0x00FFFFFF;

    createFoodSources(foodSources, &lastRand, problemSize);

    evaluateCostFunction(foodSources, costFunctionValues, weights, distances, problemSize);
    //if(localId == 0){
    //    printf("group id =%d, best id =%d, tries = %d\n",get_group_id(0), getLocalIdInSerializedResult(bestIndicator), tries[getLocalIdInSerializedResult(bestIndicator)]);
    //}

    for(iteration = 0; iteration < maxIterations; iteration++){
        employeeBees( foodSources,  costFunctionValues , tries , weights,  distances,  problemSize,  &lastRand);
        barrier(CLK_LOCAL_MEM_FENCE);
        switch(selectionMethod){
        case 0: //NONE
            break;
        case 1: //ELITE_SELECTION
            eliteSelection(foodSources,  costFunctionValues , tries , weights,  distances,  problemSize,  &lastRand, onlookersNumberInGroup, tmpTab);
            break;
        case 2: //ORIGINAL
            originalSelection(foodSources,  costFunctionValues , tries , weights,  distances,  problemSize,  &lastRand, onlookersNumberInGroup, tmpTab);
            break;
        case 3: //EMPLOYEE_BEE_VERSION
            employeeBees( foodSources,  costFunctionValues , tries , weights,  distances,  problemSize,  &lastRand);
            break;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        scoutBees(foodSources,tries,problemSize, &lastRand, maxTries);
        evaluateCostFunction(foodSources, costFunctionValues, weights, distances, problemSize);
        int oldBest = bestIndicator;
        if(useReductionToFindBest){
            findBestReduction(costFunctionValues, tmpTab, &bestIndicator);
        }else{
            updateLocalBestInLoop(costFunctionValues, &bestIndicator);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        if(getCostFunctionValueInSerializedResult(oldBest) > getCostFunctionValueInSerializedResult(bestIndicator)){
            event_t e = async_work_group_copy(solutionBuffer+ get_group_id(0)*problemSize, foodSources + getLocalIdInSerializedResult(bestIndicator) * problemSize , problemSize, 0);
            wait_group_events(1, &e);
        }
    }
}
