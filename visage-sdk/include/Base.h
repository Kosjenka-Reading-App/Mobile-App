///////////////////////////////////////////////////////////////////////////////
// 
// (c) Visage Technologies AB 2002 - 2023  All rights reserved. 
// 
// This file is part of visage|SDK(tm). 
// Unauthorized copying of this file, via any medium is strictly prohibited. 
// 
// No warranty, explicit or implicit, provided. 
// 
// This is proprietary software. No part of this software may be used or 
// reproduced in any form or by any means otherwise than in accordance with
// any written license granted by Visage Technologies AB. 
// 
///////////////////////////////////////////////////////////////////////////////

#ifndef __Base_h__
#define __Base_h__

typedef unsigned long vhandle; ///< Resource handle.
typedef unsigned long vflags; ///< A field of bits, for storing binary flags.
#define V_NULL_RESOURCE 0xFFFFFFFF

#ifdef _DEBUG
#define V_ASSERT(a) ( assert((a)) )
#else
#define V_ASSERT(a)
#endif

#ifdef VISAGE_STATIC
	#define VISAGE_DECLSPEC
#else

	#ifdef VISAGE_EXPORTS
		#define VISAGE_DECLSPEC __declspec(dllexport)
	#else
		#define VISAGE_DECLSPEC __declspec(dllimport)
	#endif

#endif

namespace VisageSDK
{

// forward declarations
class Vector2;
class Vector3;
class Vector;
class Quat;
class Matrix4;
class Matrix;
class Logger;
class VisageCharModel;
class VCM;
class FDP;
class FBAPMapping;
class Mesh;
class MorphTarget;
class Skeleton;
class Joint;

}

// C++
#include <cstdio>
#include <cstdlib>
#include <cmath>
#include <assert.h>
#include <algorithm>
#include <string>
#include <list>
#include <map>
#include <vector>
#include <set>
#include <fstream>
#include <sstream>


namespace VisageSDK
{

// aliases for some containers
typedef std::vector<int> IntArray; ///< Array of integers.
typedef std::vector<std::string> StringArray; ///< Array of strings.
typedef std::map<std::string, std::string> StringMap; ///< Map of strings, indexed by strings.
typedef std::map<std::string, Vector3> NodeTransMap; ///< Map of node translations / positions, indexed by node identifiers.
typedef std::map<std::string, Quat> NodeRotMap; ///< Map of node rotations / orientations, indexed by node identifiers.
typedef std::vector<Vector3> Vector3Array; ///< Array of 3D vectors.
typedef std::vector<Vector2> Vector2Array; ///< Array of 2D vectors.
typedef std::map<std::string, IntArray> VertexIndicesMap; ///< Map of vertex index lists, indexed by mesh identifiers.
typedef std::map<std::string, FBAPMapping> FBAPMappingsMap; ///< Map of animation parameter mappings, indexed by parameter names.
typedef std::map<int, Vector3> VertexPositionsMap; ///< Map of vertex positions, indexed by vertex indices in the mesh.
typedef std::map<std::string, VertexPositionsMap> MorphTargetsMap; ///< Map containing a group of morph targets, indexed by morph target names.
typedef std::map<std::string, MorphTargetsMap> MTMeshMap; ///< Map of morph target groups, indexed by mesh identifiers.
typedef std::map<std::string, Vector3Array> MeshVertexData; ///< Map of vertex arrays, indexed by mesh identifiers.
typedef std::map<std::string, Vector2Array> MeshTexCoordData; ///< Map of texture coord. arrays, indexed by mesh identifiers.

// some handy string function templates
template < class T >
std::string toString( T x )
{
	std::ostringstream oss;
	oss << x;
	return oss.str();
}
template < class T >
T fromString( std::string xstr )
{
	T x;
	std::istringstream iss( xstr );
	iss >> x;
	return x;
}

}

#endif // __Base_h__
