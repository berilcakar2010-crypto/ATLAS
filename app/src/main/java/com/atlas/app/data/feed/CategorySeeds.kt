package com.atlas.app.data.feed

import com.atlas.app.data.db.CategoryEntity

/** İlk kurulumda eklenen yerleşik kategoriler ve varsayılan anahtar sözcükleri. Kullanıcı hepsini düzenleyebilir. */
object CategorySeeds {
    private fun seed(c: Category, keywords: String, order: Int) =
        CategoryEntity(id = c.name, name = c.label, keywords = keywords, builtin = true, sortOrder = order)

    val all: List<CategoryEntity> = listOf(
        seed(Category.SCIENCE, "", 0),
        seed(Category.NEURO, "neuron*,neural,brain*,synap*,cortex,cortical,hippocamp*,neuroscience,neuroscientist*,dendrit*,alzheimer*,parkinson*,cognition,cognitive,neurodegenerat*", 1),
        seed(Category.PHYSICS, "quantum,particle*,photon*,superconduct*,higgs,relativity,condensed matter,laser*,boson*,fermion*,physicist*,entanglement", 2),
        seed(Category.COSMOLOGY, "galax*,universe,cosmic,cosmolog*,black hole*,telescope*,exoplanet*,dark matter,dark energy,big bang,mars,asteroid*,nebula*,supernova*,astronom*,astrophys*", 3),
        seed(Category.BIOLOGY, "species,genome*,genetic*,gene,genes,protein*,cell,cells,dna,rna,evolution*,bacteria*,virus*,vaccine*,microbio*,biolog*,ecosystem*", 4),
        seed(Category.CHEM, "chemi*,molecul*,catalyst*,polymer*,material*,nanomaterial*,compound*,reaction*", 5),
        seed(Category.EARTH, "earthquake*,volcan*,geolog*,tectonic*,atmospher*,mineral*,sediment*,erosion,glacier*", 6),
        seed(Category.MATH, "theorem*,conjecture*,topology,algebra*,mathemati*,proof,geometry,prime,primes", 7),
        seed(Category.TECH, "software,chip,chips,semiconductor*,robot*,internet,cyber*,smartphone*,startup*,computing,processor*,satellite*,encryption", 8),
        seed(Category.AI, "artificial intelligence,machine learning,language model*,llm,llms,chatbot*,deep learning,openai,anthropic,deepmind,generative,ai", 9),
        seed(Category.ENGINEERING, "engineer*,manufactur*,aircraft,3d print*,materials science,bridge*,prototype*,turbine*", 10),
        seed(Category.HEALTH, "disease*,patient*,clinical,cancer,vaccine*,hospital*,drug*,therapy,medic*,health*,surgery,epidemic*", 11),
        seed(Category.PSYCH, "psycholog*,behavio*,mental health,emotion*,memory,anxiety,depress*,personality,mindset", 12),
        seed(Category.SUSTAINABILITY, "renewable*,solar,wind power,clean energy,recycl*,sustainab*,electric vehicle*,battery,batteries,hydrogen,net zero,circular economy", 13),
        seed(Category.CLIMATE, "climate,warming,emission*,carbon,wildfire*,glacier*,sea level,biodiversity,ocean*,drought*,deforestation,pollution,coral,extinction", 14),
        seed(Category.ENERGY, "energy,nuclear,grid,oil,gas,wind power,electricity,power plant*,fusion,fission", 15),
        seed(Category.POLITICS, "election*,parliament*,sanction*,war,ceasefire,diplomat*,treaty,government*,president,minister*,military,nato,united nations,referendum", 16),
        seed(Category.SECURITY, "cyber*,hack*,malware,ransomware,vulnerabilit*,espionage,defense,defence,missile*,intelligence agenc*", 17),
        seed(Category.TURKEY, "turkey,türkiye,turkish,erdoğan,erdogan,ankara,istanbul,türk*", 18),
        seed(Category.ECONOMICS, "inflation,gdp,economy,economic*,trade,tariff*,market*,central bank*,interest rate*,recession,unemployment,fiscal,debt", 19),
        seed(Category.LAW, "court*,law,laws,legal,rights,justice,judge*,constitution*,supreme court,lawsuit*", 20),
        seed(Category.SOCIOLOGY, "inequality,migration,migrant*,society,societies,social*,gender,education,poverty,human rights,refugee*,demograph*,racism,discrimination", 21),
        seed(Category.EDUCATION, "universit*,student*,school*,education,scholarship*,curriculum,teacher*,campus,college*", 22),
        seed(Category.HISTORY, "histor*,ancient,medieval,archaeolog*,empire*,dynasty,century,ottoman,civilization*", 23),
        seed(Category.PHILOSOPHY, "philosoph*,ethic*,metaphysic*,epistemolog*,consciousness,free will,moral*,ontolog*,existential*,phenomenolog*", 24),
        seed(Category.LITERATURE, "novel*,poem*,poet*,poetry,author*,fiction,literary,literature,translation*,memoir*,essayist*", 25),
        seed(Category.LANGUAGE, "language*,linguist*,word,words,dictionary,grammar,etymolog*,vocabulary,translation*", 26),
        seed(Category.ARTS, "art,artist*,museum*,painting*,film*,music*,exhibition*,theatre,theater,architecture,sculpture*", 27),
    )
}
