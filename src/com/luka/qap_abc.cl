uint rand1(uint seed1, uint seed2){
    uint seed = seed1;
    uint t = seed ^ (seed << 11);
    return seed2 ^ (seed2 >> 19) ^ (t ^ (t >> 8));
}

uint rand2(ulong seed){
    ulong t = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    return t >> 16;
}

int get2DFrom1D(const global int* array1D, int i, int j, int arraySize){
    return array1D[j*arraySize+i];
}

kernel void generatePseudorandom(global int* a, int numElements, ulong hostSeed) {

    // get index into global data array
    int iGID = get_global_id(0);

    // bound check (equivalent to the limit on a 'for' loop)
    if (iGID >= numElements)  {
        return;
    }

    a[iGID] = rand1(hostSeed+iGID, 123456);
}

kernel void durstenfeldAlgorithm(global int* a, int permutationsNumber, int permutationLength, ulong hostSeed, local int * perm) {
    int globalId = get_global_id(0);
    int localId = get_local_id(0);
    int i, j, tmp;
    ulong lastRand = hostSeed+globalId;

    /*if(globalId>=permutationsNumber){
        return;
    }*/

    //printf("Hello from %d\n", (int)globalId);
    for(i = 0; i<permutationLength; i++){
        perm[localId * permutationLength + i] = i;
    }

    for(i = permutationLength - 1; i>0;i--){
        lastRand = rand2(lastRand);
        j =lastRand % (i+1);
        tmp = perm[localId * permutationLength + i] ;
        perm[localId * permutationLength + i] = perm[localId * permutationLength + j];
        perm[localId * permutationLength + j] = tmp;
    }
    event_t e = async_work_group_copy(a+ (globalId - localId)*permutationLength, perm , permutationLength*get_local_size(0), 0);
    wait_group_events(1, &e);
}


kernel void evaluateFitness(const global int* weights, const global int* distances, const global int* facilitiesMapping, global int* fitnessArray, int foodSourcesNumber, int problemSize) {
    int globalId = get_global_id(0);
    if (globalId >= foodSourcesNumber)  {
        return;
    }
    fitnessArray[globalId] = 0;

    /*if(globalId == 1){
        for (int i = 0; i < problemSize; i++) {
            for (int j = 0; j < problemSize; j++) {
                printf("i=%d j=%d w=%d d=%d\n", i,j,get2DFrom1D(weights,i,j, problemSize), get2DFrom1D(distances,i,j, problemSize));
            }
        }
    }*/
    for (int i = 0; i < problemSize; i++) {
        for (int j = i+1; j < problemSize; j++) {
            /*if(globalId == 1){
                printf("%d %d %d %d. Mnoze: %d * %d\n", i,j,facilitiesMapping[globalId*problemSize +i], facilitiesMapping[globalId*problemSize +j],
                get2DFrom1D(distances,i, j, problemSize),
                get2DFrom1D(weights, facilitiesMapping[globalId*problemSize +i], facilitiesMapping[globalId*problemSize +j], problemSize)
                );
            }*/
            fitnessArray[globalId] += get2DFrom1D(weights, facilitiesMapping[globalId*problemSize +i], facilitiesMapping[globalId*problemSize +j], problemSize) * get2DFrom1D(distances,i, j, problemSize);
            /*if(globalId == 1){
                printf("Mam: %d\n", fitnessArray[globalId]);
            }*/
        }
    }
}

kernel void employeeBeePhase(global int* weights, global int* distances,global int* foodSourcesInOut, global int* fitnessArray, global int* tries, int foodSourcesNumber, int problemSize, ulong hostSeed) {
    int globalId = get_global_id(0);
    if (globalId >= foodSourcesNumber)  {
        return;
    }
    int i,j;
    ulong lastRand = hostSeed+globalId;

    lastRand = rand2(lastRand);
    i = lastRand  % (problemSize);
    lastRand = rand2(lastRand);
    j =lastRand % (problemSize);

    //UWAGA mozna zrobic reuzycie zmiennej lastRand jako oldFitness
    if(globalId == 1){
        printf("EMPLOYEE BEE PHASE\noldFitness=%d\nzamiana %d z %d\n",fitnessArray[globalId],i,j);
    }

    int newFitness = fitnessArray[globalId];
    int iVal = foodSourcesInOut[globalId * problemSize + i];
    int jVal = foodSourcesInOut[globalId * problemSize + j];
    for(int a = 0;a<problemSize;a++){
        //if(a!=i && a!=j){
        //    newFitness += distances[problemSize*i+a]*(weights[jVal * problemSize + foodSourcesInOut[globalId * problemSize + a]] - weights[iVal * problemSize + foodSourcesInOut[globalId * problemSize + a]]);
        //}
        //if(a!=i && a!=j){
        //    newFitness += distances[problemSize*j+a]*(weights[iVal * problemSize + foodSourcesInOut[globalId * problemSize + a]] - weights[jVal * problemSize + foodSourcesInOut[globalId * problemSize + a]]);
        //}
        //powyższe sprowadza się do: (di - dj) * (wj - wi)
        if(a!=i && a!=j){
            newFitness+=((distances[problemSize*i+a] - distances[problemSize*j+a]) *  (weights[jVal * problemSize + foodSourcesInOut[globalId * problemSize + a]] - weights[iVal * problemSize + foodSourcesInOut[globalId * problemSize + a]]));
        }
    }

    if(newFitness < fitnessArray[globalId] ){
        fitnessArray[globalId] = newFitness;
        foodSourcesInOut[globalId * problemSize + i] = jVal;
        foodSourcesInOut[globalId * problemSize + j] = iVal;
        tries[globalId]=0;
    }else{
        tries[globalId]++;
    }
}

kernel void testIncrementkernel(global int* a,int numElements) {

    // get index into global data array
    int globalId = get_global_id(0);

    // bound check (equivalent to the limit on a 'for' loop)
    if (globalId >= numElements)  {
        return;
    }

    a[globalId] =a[globalId] + 1;
}

kernel void scoutBeePhase(global int* a, global int* tries, int threshold, int permutationsNumber, int permutationLength, ulong hostSeed, local int * perm) {
    int globalId = get_global_id(0);
    int localId = get_local_id(0);
    int i, j, tmp;
    ulong lastRand = hostSeed+globalId;

    event_t e = async_work_group_copy(perm , a+(globalId - localId)*permutationLength , permutationLength*get_local_size(0), 0);
    wait_group_events(1, &e);

    if(tries[globalId]>= threshold){
        printf("Hello from %d\n", (int)globalId);
        for(i = 0; i<permutationLength; i++){
            perm[localId * permutationLength + i] = i;
        }

        for(i = permutationLength - 1; i>0;i--){
            lastRand = rand2(lastRand);
            j =lastRand % (i+1);
            tmp = perm[localId * permutationLength + i] ;
            perm[localId * permutationLength + i] = perm[localId * permutationLength + j];
            perm[localId * permutationLength + j] = tmp;
        }
        tries[globalId] =0;
    }
    e = async_work_group_copy(a+ (globalId - localId)*permutationLength, perm , permutationLength*get_local_size(0), 0);
    wait_group_events(1, &e);
}

kernel void findBestReduction(global int* tab, global int* bestVal,  global int* bestIndex, int elements, local int * localTab, local int * localIndexTab) {
    int globalId = get_global_id(0)+get_local_size(0)*get_group_id(0);
    int localId = get_local_id(0);
    // Load data into local memory
    localTab[localId] = tab[globalId];
    localIndexTab[localId]=globalId;
    if (globalId + get_local_size(0) < elements && tab[globalId]>tab[globalId+ get_local_size(0)]) {
        localTab[localId] = tab[globalId+get_local_size(0)];
        localIndexTab[localId]=globalId+get_local_size(0);
    }

    barrier(CLK_LOCAL_MEM_FENCE | CLK_GLOBAL_MEM_FENCE);
    for(int offset = get_local_size(0) / 2;offset >0;offset >>= 1) {
        if (localId < offset) {
            printf("hello from %d. Here is %d, offset = %d and value there = %d.\n",globalId, localTab[localId], offset, localTab[localId+offset]);

            int other = localTab[localId + offset];
            int otherIndex = localIndexTab[localId + offset];
            int mine = localTab[localId];
            if(mine > other){
                localTab[localId] = other;
                localIndexTab[localId]= otherIndex;
            }
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }

    if (localId == 0) {
        bestVal[get_group_id(0)] = localTab[0];
        bestIndex[get_group_id(0)] =localIndexTab[0];
    }
}

//klamra do niczego nie pasująca ale bez niej się nie kompiluje...
}